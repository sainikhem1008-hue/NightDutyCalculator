package com.khem.nightdutycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NightDutyCalculatorScreen() }
    }
}

@Composable
fun NightDutyCalculatorScreen() {
    var dutyDate by remember { mutableStateOf("") }
    var fromTime by remember { mutableStateOf("") }
    var toTime by remember { mutableStateOf("") }
    var ceilingLimit by remember { mutableStateOf("") }
    var basicPay by remember { mutableStateOf("") }
    var daPercent by remember { mutableStateOf("") }
    var isNationalHoliday by remember { mutableStateOf(false) }
    var isWeeklyRest by remember { mutableStateOf(false) }
    var totalDutyHours by remember { mutableStateOf(0.0) }
    var nightDutyHours by remember { mutableStateOf(0.0) }
    var allowance by remember { mutableStateOf(0.0) }

    Scaffold(topBar = { TopAppBar(title = { Text("Night Duty Calculator") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .background(Color.White)
        ) {
            Text("Calculate and track your allowances", style = MaterialTheme.typography.h6)

            OutlinedTextField(
                value = dutyDate, onValueChange = { dutyDate = it },
                label = { Text("Duty Date") }, modifier = Modifier.fillMaxWidth()
            )

            Row {
                OutlinedTextField(
                    value = fromTime,
                    onValueChange = { fromTime = it },
                    label = { Text("From Time (HH:mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = toTime,
                    onValueChange = { toTime = it },
                    label = { Text("To Time (HH:mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
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
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isWeeklyRest, onCheckedChange = { isWeeklyRest = it })
                Text("Weekly Rest", modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(12.dp))

            Row {
                Button(onClick = {
                    val totalHours = calculateTimeDiff(fromTime, toTime)
                    totalDutyHours = totalHours

                    nightDutyHours = calculateNightDutyHours(fromTime, toTime)

                    val pay = basicPay.toDoubleOrNull() ?: 0.0
                    val da = pay * (daPercent.toDoubleOrNull() ?: 0.0) / 100.0
                    val hourlyRate = (pay + da) / 200.0
                    allowance = if (pay <= (ceilingLimit.toDoubleOrNull() ?: pay)) hourlyRate * nightDutyHours else 0.0
                }, modifier = Modifier.weight(1f)) {
                    Text("Calculate Allowance")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { /* Leave management action */ }, modifier = Modifier.weight(1f)) {
                    Text("Leave Management")
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Text("Total Duty Hours: ${String.format("%.2f", totalDutyHours)} hrs")
            Text("Night Hours (22:00-00:00): ${String.format("%.2f", calculateNightHours2200To0000(fromTime, toTime))} hrs")
            Text("Night Hours (00:00-06:00): ${String.format("%.2f", calculateNightHours0000To0600(fromTime, toTime))} hrs")
            Text("Total Night Hours: ${String.format("%.2f", nightDutyHours)} hrs")
            Text("Night Allowance: \u20B9${String.format("%.2f", allowance)}")

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { /* Save logic */ }) { Text("SAVE") }
                Button(onClick = { /* Export PDF logic */ }) { Text("EXPORT PDF") }
                Button(onClick = { /* Clear all fields logic */ }) { Text("CLEAR ALL") }
                Button(onClick = { /* Exit logic */ }) { Text("EXIT") }
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            // History cards (mock)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                backgroundColor = Color(0xFFE0E0E0)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("01/09/2025 | 16:00-00:00 | ₹112.63 | Regular")
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                backgroundColor = Color(0xFFE0E0E0)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("02/09/2025 | 00:00-08:00 | ₹337.90 | Regular")
                }
            }
        }
    }
}

// --- Helper Functions (all top-level, not nested) ---
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

