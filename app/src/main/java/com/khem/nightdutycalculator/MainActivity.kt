package com.khem.nightdutycalculator

import android.os.Bundle import android.widget.Toast import androidx.activity.ComponentActivity import androidx.activity.compose.setContent import androidx.compose.foundation.layout.* import androidx.compose.foundation.lazy.LazyColumn import androidx.compose.foundation.lazy.items import androidx.compose.foundation.text.KeyboardOptions import androidx.compose.material3.* import androidx.compose.runtime.* import androidx.compose.ui.Alignment import androidx.compose.ui.Modifier import androidx.compose.ui.platform.LocalContext import androidx.compose.ui.text.input.KeyboardType import androidx.compose.ui.unit.dp import androidx.navigation.NavType import androidx.navigation.compose.NavHost import androidx.navigation.compose.composable import androidx.navigation.compose.rememberNavController import androidx.navigation.navArgument import androidx.room.* import kotlinx.coroutines.launch import java.time.* import java.time.format.DateTimeFormatter

/**********************

ROOM: ENTITIES **********************/ @Entity(tableName = "duty_table") data class DutyEntity( @PrimaryKey(autoGenerate = true) val id: Int = 0, val dutyDate: String,          // ISO yyyy-MM-dd val fromTime: String,          // HH:mm val toTime: String,            // HH:mm val totalHours: Double, val nightHours: Double, val basicPayCapped: Int, val daPercent: Double, val ndaAmount: Double )


@Entity(tableName = "leave_table") data class LeaveEntity( @PrimaryKey(autoGenerate = true) val id: Int = 0, val leaveType: String, val startDate: String,         // ISO yyyy-MM-dd val endDate: String            // ISO yyyy-MM-dd )

/**********************

ROOM: DAOs **********************/ @Dao interface DutyDao { @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(duty: DutyEntity): Long

@Update suspend fun update(duty: DutyEntity)

@Query("SELECT * FROM duty_table ORDER BY dutyDate DESC, id DESC") suspend fun getAll(): List<DutyEntity>

@Query("SELECT * FROM duty_table WHERE id = :id") suspend fun getById(id: Int): DutyEntity?

@Delete suspend fun delete(duty: DutyEntity) }


@Dao interface LeaveDao { @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(leave: LeaveEntity): Long

@Update
suspend fun update(leave: LeaveEntity)

@Query("SELECT * FROM leave_table ORDER BY startDate DESC, id DESC")
suspend fun getAll(): List<LeaveEntity>

@Query("SELECT * FROM leave_table WHERE id = :id")
suspend fun getById(id: Int): LeaveEntity?

@Delete
suspend fun delete(leave: LeaveEntity)

}

/**********************

ROOM: DATABASE **********************/ @Database(entities = [DutyEntity::class, LeaveEntity::class], version = 2, exportSchema = false) abstract class AppDatabase : RoomDatabase() { abstract fun dutyDao(): DutyDao abstract fun leaveDao(): LeaveDao

companion object { @Volatile private var INSTANCE: AppDatabase? = null fun getInstance(context: android.content.Context): AppDatabase = INSTANCE ?: synchronized(this) { Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nda_db") .fallbackToDestructiveMigration() .build().also { INSTANCE = it } } } }


/**********************

ACTIVITY **********************/ class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState) val db = AppDatabase.getInstance(this) setContent { MaterialTheme { val nav = rememberNavController() NavHost(navController = nav, startDestination = "home") { composable("home") { HomeScreen(nav, db) } composable("leave") { LeaveScreen(nav, db, editId = null) } composable("history") { HistoryScreen(nav, db) } composable( route = "editDuty/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType }) ) { backStackEntry -> val id = backStackEntry.arguments?.getInt("id")!! EditDutyScreen(nav, db, id) } composable( route = "editLeave/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType }) ) { backStackEntry -> val id = backStackEntry.arguments?.getInt("id")!! LeaveScreen(nav, db, editId = id) } } } } } }


/**********************

BUSINESS HELPERS **********************/ private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_DATE


private fun parseTime(s: String): LocalTime = try { LocalTime.parse(s) } catch (: Exception) { LocalTime.MIDNIGHT } private fun parseDate(s: String): LocalDate = try { LocalDate.parse(s, ISO_DATE) } catch (: Exception) { LocalDate.now() }

// Total hours between from..to, allowing overnight (spanning midnight) private fun totalHours(from: String, to: String): Double { val f = parseTime(from) val t = parseTime(to) val end = if (!t.isBefore(f)) t else t.plusHours(24) return java.time.Duration.between(f, end).toMinutes().toDouble() / 60.0 }

// Night hours as overlap between duty interval and [22:00, 06:00] over possible 2-day span private fun nightHoursBetween(from: String, to: String): Double { val start = parseTime(from) val rawEnd = parseTime(to) val end = if (!rawEnd.isBefore(start)) rawEnd else rawEnd.plusHours(24)

// windows: [22:00, 30:00] == 22:00..(06:00 next day)
val nightStart = LocalTime.of(22, 0)
val nightEnd = LocalTime.of(6, 0).plusHours(24) // 30:00

val dutyStartMin = 0L
val dutyEndMin = java.time.Duration.between(start, end).toMinutes()

val nsMin = java.time.Duration.between(start, nightStart).toMinutes().coerceAtLeast(0)
val neMin = java.time.Duration.between(start, nightEnd).toMinutes()

val overlapStart = nsMin.coerceAtLeast(0)
val overlapEnd = neMin.coerceAtMost(dutyEndMin)
val overlap = (overlapEnd - overlapStart).coerceAtLeast(0)

return overlap.toDouble() / 60.0

}

private fun computeNdaAmount(basicPayInput: Int, daPercent: Double, nightHrs: Double): Pair<Int, Double> { val capped = if (basicPayInput > 43600) 43600 else basicPayInput val daValue = capped * (daPercent / 100.0) val hourlyRate = (capped + daValue) / 200.0 val nda = hourlyRate * (nightHrs / 6.0) return capped to nda }

private suspend fun isDateWithinAnyLeave(db: AppDatabase, dutyDateIso: String): Boolean { val date = parseDate(dutyDateIso) val leaves = db.leaveDao().getAll() return leaves.any { leave -> val s = parseDate(leave.startDate) val e = parseDate(leave.endDate) !date.isBefore(s) && !date.isAfter(e) } }

/**********************

HOME (Calculator) SCREEN **********************/ @OptIn(ExperimentalMaterial3Api::class) @Composable fun HomeScreen(nav: androidx.navigation.NavController, db: AppDatabase) { val ctx = LocalContext.current val scope = rememberCoroutineScope()

var dutyDate by remember { mutableStateOf(LocalDate.now().format(ISO_DATE)) } var fromTime by remember { mutableStateOf("22:00") } var toTime by remember { mutableStateOf("06:00") } var basicPay by remember { mutableStateOf("43600") } var daPercent by remember { mutableStateOf("55.0") }

var calcPreview by remember { mutableStateOf("") }

fun refreshPreview() { val total = totalHours(fromTime, toTime) val night = nightHoursBetween(fromTime, toTime) val cappedAndNda = computeNdaAmount(basicPay.toIntOrNull() ?: 0, daPercent.toDoubleOrNull() ?: 0.0, night) calcPreview = "Total: %.2f h, Night: %.2f h, Basic(capped): %d, NDA: ₹%.2f".format(total, night, cappedAndNda.first, cappedAndNda.second) }

LaunchedEffect(fromTime, toTime, basicPay, daPercent) { refreshPreview() }

Scaffold( topBar = { CenterAlignedTopAppBar( title = { Text("Night Duty Calculator") }, actions = { TextButton(onClick = { nav.navigate("leave") }) { Text("Leave") } TextButton(onClick = { nav.navigate("history") }) { Text("History") } } ) } ) { padding -> Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField( value = dutyDate, onValueChange = { dutyDate = it }, label = { Text("Duty Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth() ) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField( value = fromTime, onValueChange = { fromTime = it }, label = { Text("From (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f) ) OutlinedTextField( value = toTime, onValueChange = { toTime = it }, label = { Text("To (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f) ) } Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField( value = basicPay, onValueChange = { basicPay = it.filter { ch -> ch.isDigit() } }, label = { Text("Basic Pay (≤ 43600)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f) ) OutlinedTextField( value = daPercent, onValueChange = { daPercent = it.replace(',', '.').filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("DA %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f) ) }

Text(text = calcPreview, style = MaterialTheme.typography.bodyMedium)

     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
         Button(onClick = {
             // Save (with rules: cap basic; skip if on leave)
             scope.launch {
                 val total = totalHours(fromTime, toTime)
                 val night = nightHoursBetween(fromTime, toTime)
                 val (capped, nda) = computeNdaAmount(basicPay.toIntOrNull() ?: 0, daPercent.toDoubleOrNull() ?: 0.0, night)

                 val inLeave = isDateWithinAnyLeave(db, dutyDate)
                 if (inLeave) {
                     Toast.makeText(ctx, "No NDA: date is within leave period", Toast.LENGTH_LONG).show()
                     return@launch
                 }

                 val entity = DutyEntity(
                     dutyDate = dutyDate,
                     fromTime = fromTime,
                     toTime = toTime,
                     totalHours = total,
                     nightHours = night,
                     basicPayCapped = capped,
                     daPercent = daPercent.toDoubleOrNull() ?: 0.0,
                     ndaAmount = nda
                 )
                 db.dutyDao().insert(entity)
                 Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
             }
         }) { Text("Save") }

         OutlinedButton(onClick = { // Reset
             dutyDate = LocalDate.now().format(ISO_DATE)
             fromTime = "22:00"; toTime = "06:00"; basicPay = "43600"; daPercent = "55.0"; refreshPreview()
         }) { Text("Clear") }
     }
 }

} }


/**********************

LEAVE SCREEN (create / edit) **********************/ @OptIn(ExperimentalMaterial3Api::class) @Composable fun LeaveScreen(nav: androidx.navigation.NavController, db: AppDatabase, editId: Int?) { val ctx = LocalContext.current val scope = rememberCoroutineScope()

var leaveType by remember { mutableStateOf("Casual Leave") } var startDate by remember { mutableStateOf(LocalDate.now().format(ISO_DATE)) } var endDate by remember { mutableStateOf(LocalDate.now().plusDays(1).format(ISO_DATE)) }

// Load existing when editing LaunchedEffect(editId) { if (editId != null) { db.leaveDao().getById(editId)?.let { e -> leaveType = e.leaveType startDate = e.startDate endDate = e.endDate } } }

Scaffold( topBar = { CenterAlignedTopAppBar(title = { Text(if (editId == null) "Leave Management" else "Edit Leave") }) } ) { padding -> Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { var expanded by remember { mutableStateOf(false) } ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) { OutlinedTextField( value = leaveType, onValueChange = {}, readOnly = true, label = { Text("Leave Type") }, modifier = Modifier.menuAnchor().fillMaxWidth() ) ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { listOf("Casual Leave", "Sick Leave", "Earned Leave", "Other").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { leaveType = opt; expanded = false }) } } }

OutlinedTextField(
         value = startDate,
         onValueChange = { startDate = it },
         label = { Text("Start Date (YYYY-MM-DD)") },
         singleLine = true,
         modifier = Modifier.fillMaxWidth()
     )
     OutlinedTextField(
         value = endDate,
         onValueChange = { endDate = it },
         label = { Text("End Date (YYYY-MM-DD)") },
         singleLine = true,
         modifier = Modifier.fillMaxWidth()
     )

     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
         Button(onClick = {
             scope.launch {
                 val entity = LeaveEntity(id = editId ?: 0, leaveType = leaveType, startDate = startDate, endDate = endDate)
                 if (editId == null) db.leaveDao().insert(entity) else db.leaveDao().update(entity)
                 Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
                 nav.popBackStack()
             }
         }) { Text("Save") }

         OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
     }
 }

} }


/**********************

HISTORY SCREEN (tabs + edit/delete) **********************/ @Composable fun HistoryScreen(nav: androidx.navigation.NavController, db: AppDatabase) { val scope = rememberCoroutineScope() val ctx = LocalContext.current

var tab by remember { mutableStateOf(0) } // 0 = Duty, 1 = Leave var duties by remember { mutableStateOf(listOf<DutyEntity>()) } var leaves by remember { mutableStateOf(listOf<LeaveEntity>()) }

LaunchedEffect(tab) { scope.launch { duties = db.dutyDao().getAll() } scope.launch { leaves = db.leaveDao().getAll() } }

Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("History") }) }) { padding -> Column(Modifier.padding(padding)) { TabRow(selectedTabIndex = tab) { Tab(selected = tab == 0, onClick = { tab = 0; scope.launch { duties = db.dutyDao().getAll() } }, text = { Text("Duty History") }) Tab(selected = tab == 1, onClick = { tab = 1; scope.launch { leaves = db.leaveDao().getAll() } }, text = { Text("Leave History") }) } when (tab) { 0 -> DutyList(duties, onEdit = { nav.navigate("editDuty/${it.id}") }, onDelete = { entity -> scope.launch { db.dutyDao().delete(entity) duties = db.dutyDao().getAll() Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show() } } ) 1 -> LeaveList(leaves, onEdit = { nav.navigate("editLeave/${it.id}") }, onDelete = { entity -> scope.launch { db.leaveDao().delete(entity) leaves = db.leaveDao().getAll() Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show() } } ) } } } }


@Composable private fun DutyList(data: List<DutyEntity>, onEdit: (DutyEntity) -> Unit, onDelete: (DutyEntity) -> Unit) { if (data.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No duty records") } return } LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(data) { d -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("${d.dutyDate}  ${d.fromTime}→${d.toTime}") Text("Total: %.2f h  Night: %.2f h".format(d.totalHours, d.nightHours), style = MaterialTheme.typography.bodySmall) Text("Basic(capped): ${d.basicPayCapped}  DA%: ${"%.1f".format(d.daPercent)}  NDA: ₹${"%.2f".format(d.ndaAmount)}", style = MaterialTheme.typography.bodySmall) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onEdit(d) }) { Text("Edit") } OutlinedButton(onClick = { onDelete(d) }) { Text("Delete") } } } } } } }

@Composable private fun LeaveList(data: List<LeaveEntity>, onEdit: (LeaveEntity) -> Unit, onDelete: (LeaveEntity) -> Unit) { if (data.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No leave records") } return } LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(data) { l -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("${l.leaveType}") Text("${l.startDate} → ${l.endDate}", style = MaterialTheme.typography.bodySmall) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onEdit(l) }) { Text("Edit") } OutlinedButton(onClick = { onDelete(l) }) { Text("Delete") } } } } } } }

/**********************

EDIT DUTY SCREEN (prefilled) **********************/ @OptIn(ExperimentalMaterial3Api::class) @Composable fun EditDutyScreen(nav: androidx.navigation.NavController, db: AppDatabase, id: Int) { val ctx = LocalContext.current val scope = rememberCoroutineScope()

var entity by remember { mutableStateOf<DutyEntity?>(null) }

var dutyDate by remember { mutableStateOf("") } var fromTime by remember { mutableStateOf("") } var toTime by remember { mutableStateOf("") } var basicPay by remember { mutableStateOf("") } var daPercent by remember { mutableStateOf("") }

LaunchedEffect(id) { val loaded = db.dutyDao().getById(id) entity = loaded loaded?.let { e -> dutyDate = e.dutyDate fromTime = e.fromTime toTime = e.toTime basicPay = e.basicPayCapped.toString() daPercent = e.daPercent.toString() } }

Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Edit Duty") }) }) { padding -> Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = dutyDate, onValueChange = { dutyDate = it }, label = { Text("Duty Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = fromTime, onValueChange = { fromTime = it }, label = { Text("From (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f)) OutlinedTextField(value = toTime, onValueChange = { toTime = it }, label = { Text("To (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f)) } Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = basicPay, onValueChange = { basicPay = it.filter { ch -> ch.isDigit() } }, label = { Text("Basic Pay (≤ 43600)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f)) OutlinedTextField(value = daPercent, onValueChange = { daPercent = it.replace(',', '.').filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("DA %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f)) }

Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
         Button(onClick = {
             scope.launch {
                 val total = totalHours(fromTime, toTime)
                 val night = nightHoursBetween(fromTime, toTime)
                 val (capped, nda) = computeNdaAmount(basicPay.toIntOrNull() ?: 0, daPercent.toDoubleOrNull() ?: 0.0, night)
                 entity?.let { e ->
                     val updated = e.copy(
                         dutyDate = dutyDate,
                         fromTime = fromTime,
                         toTime = toTime,
                         totalHours = total,
                         nightHours = night,
                         basicPayCapped = capped,
                         daPercent = daPercent.toDoubleOrNull() ?: 0.0,
                         ndaAmount = nda
                     )
                     db.dutyDao().update(updated)
                     Toast.makeText(ctx, "Updated", Toast.LENGTH_SHORT).show()
                     nav.popBackStack()
                 }
             }
         }) { Text("Update") }

         OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
     }
 }

} }


