package com.khem.nightdutycalculator

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var etDutyDate: EditText
    private lateinit var etDutyFrom: EditText
    private lateinit var etDutyTo: EditText
    private lateinit var etCeilingLimit: EditText
    private lateinit var etBasicPay: EditText
    private lateinit var etDearnessAllowance: EditText
    private lateinit var cbNationalHoliday: CheckBox
    private lateinit var cbWeeklyRest: CheckBox
    private lateinit var btnCalculate: Button
    private lateinit var btnLeaveManagement: Button
    private lateinit var btnSave: Button
    private lateinit var btnExport: Button
    private lateinit var btnClear: Button
    private lateinit var btnExit: Button
    private lateinit var llResults: LinearLayout
    private lateinit var tvResults: TextView
    private lateinit var tvCeilingWarning: TextView
    private lateinit var rvRecords: RecyclerView

    private var history = mutableListOf<String>()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var selectedDate: LocalDate = LocalDate.now()
    private var fromTime: LocalTime = LocalTime.of(0, 0)
    private var toTime: LocalTime = LocalTime.of(8, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views by ID
        etDutyDate = findViewById(R.id.etDutyDate)
        etDutyFrom = findViewById(R.id.etDutyFrom)
        etDutyTo = findViewById(R.id.etDutyTo)
        etCeilingLimit = findViewById(R.id.etCeilingLimit)
        etBasicPay = findViewById(R.id.etBasicPay)
        etDearnessAllowance = findViewById(R.id.etDearnessAllowance)
        cbNationalHoliday = findViewById(R.id.cbNationalHoliday)
        cbWeeklyRest = findViewById(R.id.cbWeeklyRest)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnLeaveManagement = findViewById(R.id.btnLeaveManagement)
        btnSave = findViewById(R.id.btnSave)
        btnExport = findViewById(R.id.btnExport)
        btnClear = findViewById(R.id.btnClear)
        btnExit = findViewById(R.id.btnExit)
        llResults = findViewById(R.id.llResults)
        tvResults = findViewById(R.id.tvResults)
        tvCeilingWarning = findViewById(R.id.tvCeilingWarning)
        rvRecords = findViewById(R.id.rvRecords)

        // Default values
        etDutyDate.setText(selectedDate.format(dateFormatter))
        etDutyFrom.setText("00:00")
        etDutyTo.setText("08:00")
        etCeilingLimit.setText("43600")
        etBasicPay.setText("43600")
        etDearnessAllowance.setText("55.0")

        llResults.visibility = View.GONE
        tvCeilingWarning.visibility = View.GONE

        // Pickers
        etDutyDate.setOnClickListener { showDatePicker() }
        etDutyFrom.setOnClickListener { showTimePicker(true) }
        etDutyTo.setOnClickListener { showTimePicker(false) }

        // Calculate button logic
        btnCalculate.setOnClickListener { calculateAllowance() }

        // Leave management
        btnLeaveManagement.setOnClickListener { showLeaveDialog() }

        // Action buttons
        btnSave.setOnClickListener { saveResult() }
        btnExport.setOnClickListener { exportPdf() }
        btnClear.setOnClickListener { clearAll() }
        btnExit.setOnClickListener { finish() }

        // RecyclerView setup for records
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = HistoryAdapter(history)
    }

    private fun showDatePicker() {
        val d = selectedDate
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                etDutyDate.setText(selectedDate.format(dateFormatter))
            },
            d.year, d.monthValue - 1, d.dayOfMonth
        ).show()
    }

    private fun showTimePicker(isFrom: Boolean) {
        val time = if (isFrom) fromTime else toTime
        TimePickerDialog(this, { _, h, m ->
            val t = LocalTime.of(h, m)
            if (isFrom) {
                fromTime = t
                etDutyFrom.setText(String.format("%02d:%02d", h, m))
            } else {
                toTime = t
                etDutyTo.setText(String.format("%02d:%02d", h, m))
            }
        }, time.hour, time.minute, true).show()
    }

    private fun calculateAllowance() {
        val ceilingLimit = etCeilingLimit.text.toString().toDoubleOrNull() ?: 0.0
        val basicPay = etBasicPay.text.toString().toDoubleOrNull() ?: 0.0
        val daPercent = etDearnessAllowance.text.toString().toDoubleOrNull() ?: 0.0
        val isHoliday = cbNationalHoliday.isChecked
        val isRest = cbWeeklyRest.isChecked

        val totalDutyHours = calculateHours(fromTime, toTime)
        val nightHours = calculateNightHours(fromTime, toTime)
        val nda =
            ((basicPay + (daPercent / 100)) / 200) * (nightHours / 6)
        val nightAllowance = nda

        val builder = StringBuilder()
        builder.append("Total Duty Hours: %.2f hrs\n".format(totalDutyHours))
        builder.append("Night Hours (22:00-06:00): %.2f hrs\n".format(nightHours))
        builder.append("Night Allowance: ₹%.2f\n".format(nightAllowance))

        tvResults.text = builder.toString()
        llResults.visibility = View.VISIBLE

        if (basicPay > ceilingLimit) {
            tvCeilingWarning.text = "Basic Pay exceeds ceiling limit! No NDA applicable."
            tvCeilingWarning.visibility = View.VISIBLE
        } else {
            tvCeilingWarning.visibility = View.GONE
        }
    }

    private fun calculateHours(from: LocalTime, to: LocalTime): Double {
        // Handles overnight shifts
        val duration = if (to.isAfter(from)) {
            to.toSecondOfDay() - from.toSecondOfDay()
        } else {
            24 * 3600 - from.toSecondOfDay() + to.toSecondOfDay()
        }
        return duration / 3600.0
    }

    private fun calculateNightHours(from: LocalTime, to: LocalTime): Double {
        val allHours = mutableListOf<Int>()
        var current = from.hour
        val total = calculateHours(from, to).toInt()
        repeat(total) {
            allHours.add((current + it) % 24)
        }
        return allHours.count { it in 22..23 || it in 0..5 }.toDouble()
    }

    private fun showLeaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Leave Management")
        val input = EditText(this)
        input.hint = "Add Leave Note"
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val leaveNote = input.text.toString()
            if (leaveNote.isNotBlank()) {
                history.add("Leave: $leaveNote")
                rvRecords.adapter?.notifyDataSetChanged()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveResult() {
        val result = tvResults.text.toString()
        if (result.isNotBlank()) {
            history.add(result)
            rvRecords.adapter?.notifyDataSetChanged()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportPdf() {
        // Stub: Show Toast for now
        Toast.makeText(this, "Export PDF feature not implemented.", Toast.LENGTH_SHORT).show()
    }

    private fun clearAll() {
        etDutyDate.setText(LocalDate.now().format(dateFormatter))
        etDutyFrom.setText("00:00")
        etDutyTo.setText("08:00")
        etCeilingLimit.setText("43600")
        etBasicPay.setText("43600")
        etDearnessAllowance.setText("55.0")
        cbNationalHoliday.isChecked = false
        cbWeeklyRest.isChecked = false
        llResults.visibility = View.GONE
        tvCeilingWarning.visibility = View.GONE
    }

    // Simple adapter for history records
    class HistoryAdapter(private val items: List<String>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = items[position]
        }

        override fun getItemCount(): Int = items.size
    }
}
