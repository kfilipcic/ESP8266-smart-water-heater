package com.example.rijekasmarthomeapp

import android.app.DatePickerDialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.util.*

class DeviceDialog : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_dialog)

        val factor: Int = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 500)
        this.title = this.intent.getStringExtra("title")


        val startDate: TextView = findViewById(R.id.startDate)
        val startTime: TextView = findViewById(R.id.startTime)
        val endTime: TextView = findViewById(R.id.endTime)
        val cancelBtn: Button = findViewById(R.id.cancelDialogBtn)
        val okBtn: Button = findViewById(R.id.okDialogBtn)

        val currentTimeWithSeconds: String = Calendar.getInstance().time.toString().split(" ")[3]

        @Suppress("SpellCheckingInspection")
        val currTimeSplitted: List<String> = currentTimeWithSeconds.split(":")

        val currentTimeWithoutSeconds: String = currTimeSplitted[0] + ":" + currTimeSplitted[1]
        var addHour: Int = (currTimeSplitted[0].toInt() + 1)
        if (addHour >= 24) addHour = 0
        val currentTimeWithoutSeconds1: String
        currentTimeWithoutSeconds1 =
            if (addHour <= 10) "0" + addHour.toString() + ":" + currTimeSplitted[1]
            else addHour.toString() + ":" + currTimeSplitted[1]

        startTime.text = currentTimeWithoutSeconds
        endTime.text = currentTimeWithoutSeconds1

        startDate.setOnClickListener {
            // Get Current Date
            val c: Calendar = Calendar.getInstance()
            val year: Int = c.get(Calendar.YEAR)
            val month: Int = c.get(Calendar.MONTH)
            val day: Int = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog =
                DatePickerDialog(this,
                    DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                        val dateString: String = dayOfMonth.toString() + "." + (monthOfYear + 1) + "." + year
                        startDate.setText(dateString)
                    }, year, month, day
                )

            datePickerDialog.show()
        }

        cancelBtn.setOnClickListener {
            //onBackPressed()
            this.finish()
        }

        okBtn.setOnClickListener {
            this.finish()
        }

    }


}
