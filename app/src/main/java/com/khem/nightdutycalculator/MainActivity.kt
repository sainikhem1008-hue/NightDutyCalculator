package com.khem.nightdutycalculator
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
        Surface(color = Color(0xFFF5F5F5), modifier = Modifier.fillMaxSize()) {
            NightDutyCalculatorScreen()
        }
    }
}

@Composable
fun NightDutyCalculatorScreen() {
    val scrollState = rememberScrollState()
    var dutyDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var fromTime by remember { mutableStateOf("22:00") }
    var toTime by remember { mutableStateOf("06:00") }
    var showFromTimePicker by remember { mutableStateOf(false) }
    var showToTimePicker by remember { mutableStateOf(false) }

    var ceilingLimit by remember { mutableStateOf("43600") }
    var basicPay by remember { mutableStateOf("43600") }
    var daPercent by remember { mutableStateOf("55.0") }
    var isNationalHoliday by remember { mutableStateOf(false) }
    var isWeeklyRest by remember { mutableStateOf(false) }

    var totalDutyHours by remember { mutableStateOf(0.0) }
    var nightDutyHours by remember { mutableStateOf(0.0) }
    var allowance by remember { mutableStateOf(0.0) }
    var ndaAmount by remember { mutableStateOf(0.0) }
    var reportText by remember { mutableStateOf("") }

    var leaveEntries by remember { mutableStateOf(listOf<String>()) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var newLeaveEntry by remember { mutableStateOf("") }

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
            title = { Text("Leave Management") },
            text = {
                Column {
                    Text("Add Leave Note:")
                    OutlinedTextField(
                        value = newLeaveEntry,
                        onValueChange = { newLeaveEntry = it },
                        label = { Text("Leave Note") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (newLeaveEntry.isNotBlank()) {
                            leaveEntries = leaveEntries + newLeaveEntry
                            newLeaveEntry = ""
                        }
                    }) { Text("Add") }
                    Spacer(Modifier.height(8.dp))
                    Text("Current Leaves:", fontWeight = FontWeight.Bold)
                    leaveEntries.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp)
    ) {

        // 1. Blue Card Header
        Card(
            backgroundColor = Color(0xFF1976D2),
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Night Duty Calculator",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        // 2. Date Picker (only via calendar)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dutyDate.format(DateTimeFormatter.ISO_DATE),
                onValueChange = {},
                label = { Text("Duty Date") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFFE3F2FD)
                )
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Pick Date")
            }
        }
        Spacer(Modifier.height(8.dp))

        // 3. Time Picker From Clock With Manual Entry
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = fromTime,
                onValueChange = { fromTime = it },
                label = { Text("From Time (HH:mm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFFE3F2FD)
                )
            )
            IconButton(onClick = { showFromTimePicker = true }) {
                Icon(Icons.Filled.Schedule, contentDescription = "Pick From Time")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = toTime,
                onValueChange = { toTime = it },
                label = { Text("To Time (HH:mm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFFE3F2FD)
                )
            )
            IconButton(onClick = { showToTimePicker = true }) {
                Icon(Icons.Filled.Schedule, contentDescription = "Pick To Time")
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = ceilingLimit,
            onValueChange = { ceilingLimit = it },
            label = { Text("Ceiling Limit (\u20B9)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = basicPay,
            onValueChange = { basicPay = it },
            label = { Text("Basic Pay (\u20B9)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = daPercent,
            onValueChange = { daPercent = it },
            label = { Text("Dearness Allowance (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isNationalHoliday, onCheckedChange = { isNationalHoliday = it })
            Text("National Holiday", modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.width(24.dp))
            Checkbox(checked = isWeeklyRest, onCheckedChange = { isWeeklyRest = it })
            Text("Weekly Rest", modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.height(16.dp))

        // 8. Major Action Buttons (Top Section)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Button(
                onClick = {
                    totalDutyHours = calculateTimeDiff(fromTime, toTime)
                    nightDutyHours = calculateNightDutyHours(fromTime, toTime)
                    val pay = basicPay.toDoubleOrNull() ?: 0.0
                    val daPerc = daPercent.toDoubleOrNull() ?: 0.0
                    val da = (pay * daPerc / 100.0)
                    allowance = if (pay <= (ceilingLimit.toDoubleOrNull() ?: pay)) ((pay + daPerc) / 200.0) * (nightDutyHours / 6.0) else 0.0
                    ndaAmount = ((pay + (daPerc)) / 200.0) * (nightDutyHours / 6.0)
                    reportText = generateReport(
                        dutyDate = dutyDate.format(DateTimeFormatter.ISO_DATE),
                        fromTime = fromTime,
                        toTime = toTime,
                        totalDutyHours = totalDutyHours,
                        nightDutyHours = nightDutyHours,
                        allowance = allowance,
                        ndaAmount = ndaAmount,
                        basicPay = pay,
                        daPercent = daPercent,
                        ceilingLimit = ceilingLimit,
                        isNationalHoliday = isNationalHoliday,
                        isWeeklyRest = isWeeklyRest,
                        leaveEntries = leaveEntries
                    )
                }, modifier = Modifier.weight(1f)
            ) { Text("Calculate") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { showLeaveDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("Leave Management") }
        }

        Spacer(Modifier.height(16.dp))
        Divider()

        // 8. Secondary Action Buttons (Bottom Section, grouped)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Button(onClick = {
                // Save: Add current report to local history 
                // (extend logic as needed)
            }) { Text("SAVE") }
            Button(onClick = {
                // Export PDF: Stub, use PDF library for full logic
            }) { Text("EXPORT PDF") }
            Button(onClick = {
                dutyDate = LocalDate.now()
                fromTime = "22:00"
                toTime = "06:00"
                ceilingLimit = "43600"
                basicPay = "43600"
                daPercent = "55.0"
                isNationalHoliday = false
                isWeeklyRest = false
                totalDutyHours = 0.0
                nightDutyHours = 0.0
                allowance = 0.0
                ndaAmount = 0.0
                reportText = ""
                leaveEntries = emptyList()
                newLeaveEntry = ""
            }) { Text("CLEAR ALL") }
            Button(onClick = {
                // Exit: In real app, call finish()
                // For Compose preview, no-op
            }) { Text("EXIT") }
        }

        Spacer(Modifier.height(12.dp))
        Divider()

        // Report section, scrollable
        if (reportText.isNotEmpty()) {
            Text("Report", style = MaterialTheme.typography.h6)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                backgroundColor = Color(0xFFE0E0E0),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(reportText)
                }
            }
        } else {
            Text("No report generated. Fill fields and tap Calculate.")
        }
    }
}

// --- Date & Time Picker Dialogs ---
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

// --- NDA Calculation Helpers ---
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
    totalDutyHours: Double, nightDutyHours: Double, allowance: Double, ndaAmount: Double,
    basicPay: Double, daPercent: String, ceilingLimit: String,
    isNationalHoliday: Boolean, isWeeklyRest: Boolean, leaveEntries: List<String>
): String {
    return buildString {
        appendLine("Date: $dutyDate")
        appendLine("Duration: $fromTime to $toTime")
        appendLine("Total Duty Hours: %.2f".format(totalDutyHours))
        appendLine("Total Night Duty Hours: %.2f".format(nightDutyHours))
        appendLine("NDA (Exact): \u20B9%.2f".format(ndaAmount))
        appendLine("Allowance: \u20B9%.2f".format(allowance))
        appendLine("Basic Pay: \u20B9%.2f".format(basicPay))
        appendLine("DA: $daPercent%")
        appendLine("Ceiling Limit: \u20B9$ceilingLimit")
        if (isNationalHoliday) appendLine("National Holiday: Yes")
        if (isWeeklyRest) appendLine("Weekly Rest: Yes")
        if (leaveEntries.isNotEmpty()) {
            appendLine("Leave Records:")
            leaveEntries.forEach { appendLine("• $it") }
        }
    }
}
