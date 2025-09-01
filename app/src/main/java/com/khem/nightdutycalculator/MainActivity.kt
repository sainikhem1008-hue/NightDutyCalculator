package com.khem.nightdutycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NightDutyCalculatorApp() }
    }
}

@Composable
fun NightDutyCalculatorApp() {
    MaterialTheme {
        Surface(color = Color(0xFFF9F9F9), modifier = Modifier.fillMaxSize()) {
            NightDutyCalculatorScreen()
        }
    }
}

@Composable
fun NightDutyCalculatorScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var dutyDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var fromTime by remember { mutableStateOf("22:00") }
    var toTime by remember { mutableStateOf("06:00") }
    var ceilingLimit by remember { mutableStateOf("43600") }
    var basicPay by remember { mutableStateOf("43600") }
    var daPercent by remember { mutableStateOf("55.0") }
    var isNationalHoliday by remember { mutableStateOf(false) }
    var isWeeklyRest by remember { mutableStateOf(false) }
    var totalDutyHours by remember { mutableStateOf(0.0) }
    var nightDutyHours by remember { mutableStateOf(0.0) }
    var allowance by remember { mutableStateOf(0.0) }
    var reportText by remember { mutableStateOf("") }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFromTimePicker by remember { mutableStateOf(false) }
    var showToTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = LocalDate.parse(dutyDate),
            onDateSelected = {
                dutyDate = it.format(DateTimeFormatter.ISO_DATE)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showFromTimePicker) {
        TimePickerDialog(
            time = fromTime,
            onTimeSelected = {
                fromTime = it
                showFromTimePicker = false
            },
            onDismiss = { showFromTimePicker = false }
        )
    }

    if (showToTimePicker) {
        TimePickerDialog(
            time = toTime,
            onTimeSelected = {
                toTime = it
                showToTimePicker = false
            },
            onDismiss = { showToTimePicker = false }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Management") },
            text = { Text("This is a placeholder for leave management. Add or view leaves here.") },
            confirmButton = {
                Button(onClick = { showLeaveDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .background(Color.White)
    ) {
        Text("Night Duty Calculator", style = MaterialTheme.typography.h5, color = Color(0xFF1976D2))
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = dutyDate,
                onValueChange = { dutyDate = it },
                label = { Text("Duty Date") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = fromTime,
                onValueChange = { fromTime = it },
                label = { Text("From Time (HH:mm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            IconButton(onClick = { showFromTimePicker = true }) {
                Icon(Icons.Default.Schedule, contentDescription = "Pick From Time")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = toTime,
                onValueChange = { toTime = it },
                label = { Text("To Time (HH:mm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            IconButton(onClick = { showToTimePicker = true }) {
                Icon(Icons.Default.Schedule, contentDescription = "Pick To Time")
            }
        }

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

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val totalHours = calculateTimeDiff(fromTime, toTime)
                    totalDutyHours = totalHours
                    nightDutyHours = calculateNightDutyHours(fromTime, toTime)
                    val pay = basicPay.toDoubleOrNull() ?: 0.0
                    val da = pay * (daPercent.toDoubleOrNull() ?: 0.0) / 100.0
                    val hourlyRate = (pay + da) / 200.0
                    allowance = if (pay <= (ceilingLimit.toDoubleOrNull() ?: pay)) hourlyRate * nightDutyHours else 0.0
                    reportText = generateReport(
                        dutyDate = dutyDate,
                        fromTime = fromTime,
                        toTime = toTime,
                        totalDutyHours = totalDutyHours,
                        nightDutyHours = nightDutyHours,
                        allowance = allowance,
                        basicPay = pay,
                        daPercent = daPercent,
                        ceilingLimit = ceilingLimit,
                        isNationalHoliday = isNationalHoliday,
                        isWeeklyRest = isWeeklyRest
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Calculate")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { showLeaveDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Leave Management")
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                // Save logic (stub)
            }) { Text("SAVE") }
            Button(onClick = {
                // Export PDF logic (stub)
            }) { Text("EXPORT PDF") }
            Button(onClick = {
                // Clear logic
                dutyDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
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
                reportText = ""
            }) { Text("CLEAR ALL") }
            Button(onClick = {
                // Exit logic (stub)
            }) { Text("EXIT") }
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        if (reportText.isNotEmpty()) {
            Text("Report", style = MaterialTheme.typography.h6)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                backgroundColor = Color(0xFFE0E0E0)
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

// Simple Compose DatePicker dialog using system picker
@Composable
fun DatePickerDialog(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val datePicker = remember { android.app.DatePickerDialog(
        LocalContext.current,
        { _, year, month, day ->
            onDateSelected(LocalDate.of(year, month + 1, day))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ) }
    DisposableEffect(Unit) {
        datePicker.setOnCancelListener { onDismiss() }
        datePicker.show()
        onDispose { }
    }
}

// Simple Compose TimePicker dialog using system picker
@Composable
fun TimePickerDialog(time: String, onTimeSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val parsedTime = try { LocalTime.parse(time) } catch (e: Exception) { LocalTime.of(0,0) }
    val timePicker = remember { android.app.TimePickerDialog(
        LocalContext.current,
        { _, hour, minute ->
            onTimeSelected("%02d:%02d".format(hour, minute))
        },
        parsedTime.hour,
        parsedTime.minute,
        true
    ) }
    DisposableEffect(Unit) {
        timePicker.setOnCancelListener { onDismiss() }
        timePicker.show()
        onDispose { }
    }
}

// Generates summary report string
fun generateReport(
    dutyDate: String, fromTime: String, toTime: String,
    totalDutyHours: Double, nightDutyHours: Double, allowance: Double,
    basicPay: Double, daPercent: String, ceilingLimit: String,
    isNationalHoliday: Boolean, isWeeklyRest: Boolean
): String {
    return buildString {
        appendLine("Date: $dutyDate")
        appendLine("Duration: $fromTime to $toTime")
        appendLine("Total Duty: %.2f hrs".format(totalDutyHours))
        appendLine("Night Duty: %.2f hrs".format(nightDutyHours))
        appendLine("Allowance: \u20B9%.2f".format(allowance))
        appendLine("Basic Pay: \u20B9%.2f".format(basicPay))
        appendLine("DA: $daPercent%")
        appendLine("Ceiling Limit: \u20B9$ceilingLimit")
        if (isNationalHoliday) appendLine("National Holiday: Yes")
        if (isWeeklyRest) appendLine("Weekly Rest: Yes")
    }
}

// --- NDA Logic & Helpers ---
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

fun calculateNightHours2200To0000(from: String, to: String): Double {
    return getHourlySegments(from, to).count { it in 22..23 }.toDouble()
}

fun calculateNightHours0000To0600(from: String, to: String): Double {
    return getHourlySegments(from, to).count { it in 0..5 }.toDouble()
}
