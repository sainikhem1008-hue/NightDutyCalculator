package com.khem.nightdutycalculator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import com.khem.nightdutycalculator.ui.theme.NightDutyCalculatorTheme   // ✅ custom theme import

/**********************
 ROOM: ENTITIES
 **********************/
@Entity(tableName = "duty_table")
data class DutyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dutyDate: String,          // ISO yyyy-MM-dd
    val fromTime: String,          // HH:mm
    val toTime: String,            // HH:mm
    val totalHours: Double,
    val nightHours: Double,
    val basicPayCapped: Int,
    val daPercent: Double,
    val ndaAmount: Double
)

@Entity(tableName = "leave_table")
data class LeaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leaveType: String,
    val startDate: String,         // ISO yyyy-MM-dd
    val endDate: String            // ISO yyyy-MM-dd
)

/**********************
 ROOM: DAOs
 **********************/
@Dao
interface DutyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(duty: DutyEntity): Long
    @Update suspend fun update(duty: DutyEntity)
    @Query("SELECT * FROM duty_table ORDER BY dutyDate DESC, id DESC") suspend fun getAll(): List<DutyEntity>
    @Query("SELECT * FROM duty_table WHERE id = :id") suspend fun getById(id: Int): DutyEntity?
    @Delete suspend fun delete(duty: DutyEntity)
}

@Dao
interface LeaveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(leave: LeaveEntity): Long
    @Update suspend fun update(leave: LeaveEntity)
    @Query("SELECT * FROM leave_table ORDER BY startDate DESC, id DESC") suspend fun getAll(): List<LeaveEntity>
    @Query("SELECT * FROM leave_table WHERE id = :id") suspend fun getById(id: Int): LeaveEntity?
    @Delete suspend fun delete(leave: LeaveEntity)
}

/**********************
 ROOM: DATABASE
 **********************/
@Database(entities = [DutyEntity::class, LeaveEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dutyDao(): DutyDao
    abstract fun leaveDao(): LeaveDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nda_db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

/**********************
 ACTIVITY
 **********************/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(this)

        setContent {
            NightDutyCalculatorTheme {   // ✅ custom Material3 theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") { HomeScreen(nav, db) }
                        composable("leave") { LeaveScreen(nav, db, editId = null) }
                        composable("history") { HistoryScreen(nav, db) }
                        composable(
                            route = "editDuty/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id")!!
                            EditDutyScreen(nav, db, id)
                        }
                        composable(
                            route = "editLeave/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id")!!
                            LeaveScreen(nav, db, editId = id)
                        }
                    }
                }
            }
        }
    }
}

/**********************
 BUSINESS HELPERS
 **********************/
private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_DATE

private fun parseTime(s: String): LocalTime = try { LocalTime.parse(s) } catch (_: Exception) { LocalTime.MIDNIGHT }
private fun parseDate(s: String): LocalDate = try { LocalDate.parse(s, ISO_DATE) } catch (_: Exception) { LocalDate.now() }

private fun totalHours(from: String, to: String): Double {
    val f = parseTime(from)
    val t = parseTime(to)
    val end = if (!t.isBefore(f)) t else t.plusHours(24)
    return java.time.Duration.between(f, end).toMinutes().toDouble() / 60.0
}

private fun nightHoursBetween(from: String, to: String): Double {
    val start = parseTime(from)
    val rawEnd = parseTime(to)
    val end = if (!rawEnd.isBefore(start)) rawEnd else rawEnd.plusHours(24)

    val nightStart = LocalTime.of(22, 0)
    val nightEnd = LocalTime.of(6, 0).plusHours(24)

    val dutyStartMin = 0L
    val dutyEndMin = java.time.Duration.between(start, end).toMinutes()

    val nsMin = java.time.Duration.between(start, nightStart).toMinutes().coerceAtLeast(0)
    val neMin = java.time.Duration.between(start, nightEnd).toMinutes()

    val overlapStart = nsMin.coerceAtLeast(0)
    val overlapEnd = neMin.coerceAtMost(dutyEndMin)
    val overlap = (overlapEnd - overlapStart).coerceAtLeast(0)

    return overlap.toDouble() / 60.0
}

private fun computeNdaAmount(basicPayInput: Int, daPercent: Double, nightHrs: Double): Pair<Int, Double> {
    val capped = if (basicPayInput > 43600) 43600 else basicPayInput
    val daValue = capped * (daPercent / 100.0)
    val hourlyRate = (capped + daValue) / 200.0
    val nda = hourlyRate * (nightHrs / 6.0)
    return capped to nda
}

private suspend fun isDateWithinAnyLeave(db: AppDatabase, dutyDateIso: String): Boolean {
    val date = parseDate(dutyDateIso)
    val leaves = db.leaveDao().getAll()
    return leaves.any { leave ->
        val s = parseDate(leave.startDate)
        val e = parseDate(leave.endDate)
        !date.isBefore(s) && !date.isAfter(e)
    }
}

/**********************
 (All your screens: HomeScreen, LeaveScreen, HistoryScreen, EditDutyScreen, etc.)
 ✅ Leave them exactly as you pasted — no changes needed, since they already work.
 **********************/
