package com.khem.nightdutycalculator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.time.format.DateTimeFormatter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NightDutyCalculatorApp() }
    }
}

   @Composable
fun NightDutyCalculatorScreen() {
    val context = LocalContext.current
    val history = remember { mutableStateListOf<String>() }
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

    // --- Date & Time Pickers ---
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

    // Leave Dialog
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
            confirmButton = { Button(onClick = { showLeaveDialog = false }) { Text("Close") } }
        )
    }

    // --- Main UI ---
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 12.dp),
        state = scrollState
    ) {
        // Header
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
                    Text("🌙 Night Duty Calculator", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Calculate and track your allowances", fontSize = 14.sp, color = Color.White)
                }
            }
        }

        // Input Fields
        item {
            OutlinedTextField(
                value = dutyDate.format(DateTimeFormatter.ISO_DATE),
                onValueChange = {},
                label = { Text("Duty Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color(0xFFE3F2FD)),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Pick Date")
                    }
                }
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = fromTime,
                    onValueChange = { fromTime = it },
                    label = { Text("From Time (HH:mm)") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    trailingIcon = { IconButton(onClick = { showFromTimePicker = true }) { Icon(Icons.Filled.Schedule, "Pick") } }
                )
                OutlinedTextField(
                    value = toTime,
                    onValueChange = { toTime = it },
                    label = { Text("To Time (HH:mm)") },
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    trailingIcon = { IconButton(onClick = { showToTimePicker = true }) { Icon(Icons.Filled.Schedule, "Pick") } }
                )
            }
        }
        item {
            OutlinedTextField(value = ceilingLimit, onValueChange = { ceilingLimit = it }, label = { Text("Ceiling Limit (\u20B9)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            OutlinedTextField(value = basicPay, onValueChange = { basicPay = it }, label = { Text("Basic Pay (\u20B9)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            OutlinedTextField(value = daPercent, onValueChange = { daPercent = it }, label = { Text("Dearness Allowance (%)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        item {
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isNationalHoliday, onCheckedChange = { isNationalHoliday = it })
                Text("National Holiday", Modifier.padding(start = 4.dp))
                Spacer(Modifier.width(24.dp))
                Checkbox(checked = isWeeklyRest, onCheckedChange = { isWeeklyRest = it })
                Text("Weekly Rest", Modifier.padding(start = 4.dp))
            }
        }

        // Buttons: Calculate & Leave
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    totalDutyHours = calculateTimeDiff(fromTime, toTime)
                    nightDutyHours = calculateNightDutyHours(fromTime, toTime)
                    val pay = basicPay.toDoubleOrNull() ?: 0.0
                    val daPerc = daPercent.toDoubleOrNull() ?: 0.0
                    val daValue = pay * daPerc / 100.0
                    ndaAmount = ((pay + daValue) / 200.0) * (nightDutyHours / 6.0)
                    nightAllowance = ndaAmount

                    reportText = generateReport(
                        dutyDate.format(DateTimeFormatter.ISO_DATE),
                        fromTime,
                        toTime,
                        totalDutyHours,
                        nightDutyHours,
                        nightAllowance,
                        ndaAmount,
                        pay,
                        daPercent,
                        ceilingLimit,
                        isNationalHoliday,
                        isWeeklyRest,
                        leaveEntries
                    )

                    warningText = if (pay > (ceilingLimit.toDoubleOrNull() ?: pay))
                        "Basic Pay exceeds ceiling limit! No NDA applicable."
                    else ""
                }, modifier = Modifier.weight(1f)) { Text("🧮 Calculate Allowance") }

                Button(onClick = { showLeaveDialog = true }, modifier = Modifier.weight(1f)) { Text("📅 Leave Management") }
            }
        }

        // Warning and Report
        item {
            if (warningText.isNotEmpty()) {
                Card(backgroundColor = Color(0xFFFF9800), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(warningText, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
            }
            if (reportText.isNotEmpty()) {
                Card(backgroundColor = Color(0xFFE0E0E0), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = 4.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(8.dp)) { Text(reportText) }
                }
            } else {
                Text("No report generated. Fill fields and tap Calculate.", Modifier.padding(8.dp))
            }
        }

        // Secondary Actions: Save, Export, Clear, Exit
        item {
            Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (reportText.isNotBlank()) {
                            history.add(reportText)
                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Calculate first!", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f)) { Text("💾 Save") }

                    Button(onClick = {
                        if (reportText.isNotBlank()) {
                            val pdfFile = generatePdf(context, reportText)
                            if (pdfFile != null) sharePdfFile(context, pdfFile)
                        }
                    }, modifier = Modifier.weight(1f)) { Text("📄 Export PDF") }
                }

                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        // Clear all
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

                    Button(onClick = { if (context is ComponentActivity) context.finish() }, modifier = Modifier.weight(1f)) { Text("🚪 Exit") }
                }
            }
        }

        // History Section
        item {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("History", style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))
            if (history.isEmpty()) {
                Text("No saved reports.", Modifier.padding(8.dp))
            } else {
                Column {
                    history.forEach { entry ->
                        Card(backgroundColor = Color(0xFFF5F5F5), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 2.dp) {
                            Text(entry, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}
