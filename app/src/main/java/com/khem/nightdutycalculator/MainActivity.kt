package com.khem.nightdutycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NightDutyCalculatorApp() }
    }
}

@Composable
fun NightDutyCalculatorApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
            NightDutyCalculatorScreen()
        }
    }
}

@Composable
fun NightDutyCalculatorScreen() {
    // State variables
    val scrollState = rememberLazyListState()

    var dutyDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var fromTime by remember { mutableStateOf("00:00") }
    var toTime by remember { mutableStateOf("08:00") }
    var showFromTimePicker by remember { mutableStateOf(false) }
    var showToTimePicker by remember { mutableStateOf(false) }

    var ceilingLimit by remember { mutableStateOf("43600") }
    var basicPay by remember { mutableStateOf("43600") }
    var daPercent by remember { mutableStateOf("55.0") }
    var isNationalHoliday by remember { mutableStateOf(false) }
    var isWeeklyRest by remember { mutableStateOf(false) }

    var totalDutyHours by remember { mutableStateOf(0.0) }
    var nightDutyHours by remember { mutableStateOf(0.0) }
    var nightAllowance by remember { mutableStateOf(0.0) }
    var ndaAmount by remember { mutableStateOf(0.0) }
    var reportText by remember { mutableStateOf("") }
    var warningText by remember { mutableStateOf("") }

    var leaveEntries by remember { mutableStateOf(listOf<String>()) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var newLeaveEntry by remember { mutableStateOf("") }

    var history by remember { mutableStateOf(listOf<String>()) }

    // --- Dialogs ---
    DatePickerDialog(
        show = showDatePicker,
        initialDate = dutyDate,
        onDateSelected = {
            dutyDate = it
            showDatePicker = false
        },
        onDismiss = { showDatePicker = false }
    )
    TimePickerDialog(
        show = showFromTimePicker,
        initialTime = fromTime,
        onTimeSelected = {
            fromTime = it
            showFromTimePicker = false
        },
        onDismiss = { showFromTimePicker = false }
    )
    TimePickerDialog(
        show = showToTimePicker,
        initialTime = toTime,
        onTimeSelected = {
            toTime = it
            showToTimePicker = false
        },
        onDismiss = { showToTimePicker = false }
    )

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Management", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newLeaveEntry,
                        onValueChange = { newLeaveEntry = it },
                        label = { Text("Add Leave Note") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (newLeaveEntry.isNotBlank()) {
                            leaveEntries = leaveEntries + newLeaveEntry
                            newLeaveEntry = ""
                        }
                    }) { Text("Add") }
                    Spacer(Modifier.height(8.dp))
                    Text("Current Leaves", fontWeight = FontWeight.Bold)
                    leaveEntries.forEachIndexed { index, entry ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry)
                            IconButton(onClick = {
                                leaveEntries = leaveEntries.filterIndexed { i, _ -> i != index }
                            }) {
                                Icon(Icons.Filled.Schedule, contentDescription = "Remove")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLeaveDialog = false }) { Text("Close") }
            }
        )
    }

    // --- Layout ---
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
        .padding(horizontal = 12.dp)
        , state = scrollState
    ) {

        // Header Card
        item {
            Card(
                backgroundColor = Color(0xFF1976D2),
                shape = RoundedCornerShape(12.dp),
                elevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "🌙 Night Duty Calculator",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Calculate and track your allowances",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        // --- Input Fields ---
        item {
            OutlinedTextField(
                value = dutyDate.format(DateTimeFormatter.ISO_DATE),
                onValueChange = {},
                label = { Text("Duty Date") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFFE3F2FD)
                ),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Pick Date")
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = fromTime,
                    onValueChange = { fromTime = it },
                    label = { Text("From Time (HH:mm)") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    keyboardOptions = KeyboardOptions.Default,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFFE3F2FD)
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showFromTimePicker = true }) {
                            Icon(Icons.Filled.Schedule, contentDescription = "Pick From Time")
                        }
                    }
                )
                OutlinedTextField(
                    value = toTime,
                    onValueChange = { toTime = it },
                    label = { Text("To Time (HH:mm)") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    keyboardOptions = KeyboardOptions.Default,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFFE3F2FD)
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showToTimePicker = true }) {
                            Icon(Icons.Filled.Schedule, contentDescription = "Pick To Time")
                        }
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        item {
            OutlinedTextField(
                value = ceilingLimit,
                onValueChange = { ceilingLimit = it },
                label = { Text("Ceiling Limit (\u20B9)") },
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = basicPay,
                onValueChange = { basicPay = it },
                label = { Text("Basic Pay (\u20B9)") },
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = daPercent,
                onValueChange = { daPercent = it },
                label = { Text("Dearness Allowance (%)") },
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Checkbox(checked = isNationalHoliday, onCheckedChange = { isNationalHoliday = it })
                Text("National Holiday", modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.width(24.dp))
                Checkbox(checked = isWeeklyRest, onCheckedChange = { isWeeklyRest = it })
                Text("Weekly Rest", modifier = Modifier.padding(start = 4.dp))
            }
        }

        // --- Major Action Buttons ---
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        totalDutyHours = calculateTimeDiff(fromTime, toTime)
                        nightDutyHours = calculateNightDutyHours(fromTime, toTime)
                        val pay = basicPay.toDoubleOrNull() ?: 0.0
                        val daPerc = daPercent.toDoubleOrNull() ?: 0.0
                        val daValue = pay * daPerc / 100.0
                        ndaAmount = ((pay + daValue) / 200.0) * (nightDutyHours / 6.0)
                        nightAllowance = ndaAmount

                        reportText = generateReport(
                            dutyDate = dutyDate.format(DateTimeFormatter.ISO_DATE),
                            fromTime = fromTime,
                            toTime = toTime,
                            totalDutyHours = totalDutyHours,
                            nightDutyHours = nightDutyHours,
                            nightAllowance = nightAllowance,
                            ndaAmount = ndaAmount,
                            basicPay = pay,
                            daPercent = daPercent,
                            ceilingLimit = ceilingLimit,
                            isNationalHoliday = isNationalHoliday,
                            isWeeklyRest = isWeeklyRest,
                            leaveEntries = leaveEntries
                        )

                        warningText =
                            if (pay > (ceilingLimit.toDoubleOrNull() ?: pay))
                                "Basic Pay exceeds ceiling limit! No NDA applicable."
                            else
                                ""
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("🧮 Calculate Allowance") }
                Button(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier.weight(1f)
                ) { Text("📅 Leave Management") }
            }
        }

        // --- Results and Warning Banner ---
        item {
            if (warningText.isNotEmpty()) {
                Card(
                    backgroundColor = Color(0xFFFF9800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = warningText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (reportText.isNotEmpty()) {
                Card(
                    backgroundColor = Color(0xFFE0E0E0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(reportText)
                    }
                }
            } else {
                Text(
                    "No report generated. Fill fields and tap Calculate.",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // --- Secondary Actions: Save/Export and Clear/Exit ---
        item {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Button(onClick = {
                        // Save: Add report to history
                        if (reportText.isNotBlank()) {
                            history = history + reportText
                        }
                    }, modifier = Modifier.weight(1f)) { Text("💾 Save") }
                    Button(onClick = {
                        // Export PDF: Stub, extend as needed
                    }, modifier = Modifier.weight(1f)) { Text("📄 Export PDF") }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Button(onClick = {
                        dutyDate = LocalDate.now()
                        fromTime = "00:00"
                        toTime = "08:00"
                        ceilingLimit = "43600"
                        basicPay = "43600"
                        daPercent = "55.0"
                        isNationalHoliday = false
                        isWeeklyRest = false
                        totalDutyHours = 0.0
                        nightDutyHours = 0.0
                        nightAllowance = 0.0
                        ndaAmount = 0.0
                        reportText = ""
                        warningText = ""
                        leaveEntries = emptyList()
                        newLeaveEntry = ""
                    }, modifier = Modifier.weight(1f)) { Text("🗑️ Clear All") }
                    Button(onClick = {
                        // Exit: Call finish() from activity, if needed
                    }, modifier = Modifier.weight(1f)) { Text("🚪 Exit") }
                }
            }
        }

        // --- Record/History List ---
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("History", style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))

            if (history.isEmpty()) {
                Text("No saved reports.", modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn {
                    items(history) { entry ->
                        Card(
                            backgroundColor = Color(0xFFF5F5F5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = 2.dp
                        ) {
                            Text(entry, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- Date & Time Picker Dialogs (Compose helper functions) ---
@Composable
fun DatePickerDialog(show: Boolean, initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    if (show) {
        val context = LocalContext.current
        LaunchedEffect(show) {
            android.app.DatePickerDialog(
                context,
                { _, year, month, day ->
                    onDateSelected(LocalDate.of(year, month + 1, day))
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth
            ).apply {
                setOnCancelListener { onDismiss() }
            }.show()
        }
    }
}

@Composable
fun TimePickerDialog(show: Boolean, initialTime: String, onTimeSelected: (String) -> Unit, onDismiss: () -> Unit) {
    if (show) {
        val context = LocalContext.current
        val parsedTime = try { LocalTime.parse(initialTime) } catch (e: Exception) { LocalTime.of(0, 0) }
        LaunchedEffect(show) {
            android.app.TimePickerDialog(
                context,
                { _, hour, minute ->
                    onTimeSelected(String.format("%02d:%02d", hour, minute))
                },
                parsedTime.hour,
                parsedTime.minute,
                true
            ).apply {
                setOnCancelListener { onDismiss() }
            }.show()
        }
    }
}

// --- Helpers ---
fun calculateTimeDiff(from: String, to: String): Double {
    return try {
        val fromT = LocalTime.parse(from)
        val toT = LocalTime.parse(to)
        val diff = if (toT.isAfter(fromT)) {
            toT.toSecondOfDay() - fromT.toSecondOfDay()
        } else {
            (24 * 60 * 60 - fromT.toSecondOfDay()) + toT.toSecondOfDay()
        }
        diff / 3600.0
    } catch (e: Exception) { 0.0 }
}

fun calculateNightDutyHours(from: String, to: String): Double {
    val allHours = getHourlySegments(from, to)
    return allHours.count { isNightHour(it) }.toDouble()
}

fun getHourlySegments(from: String, to: String): List<Int> {
    return try {
        val fromT = LocalTime.parse(from)
        val toT = LocalTime.parse(to)
        val hours = mutableListOf<Int>()
        var current = fromT.hour
        val total = (calculateTimeDiff(from, to)).toInt()
        repeat(total) {
            hours.add((current + it) % 24)
        }
        hours
    } catch (ex: Exception) { emptyList() }
}

fun isNightHour(hour: Int): Boolean = (hour in 22..23) || (hour in 0..5)

fun generateReport(
    dutyDate: String, fromTime: String, toTime: String,
    totalDutyHours: Double, nightDutyHours: Double, nightAllowance: Double, ndaAmount: Double,
    basicP
