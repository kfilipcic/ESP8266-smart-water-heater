package com.example.rijekasmarthomeapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.*
import kotlin.collections.ArrayList

class DeviceDialog : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_dialog)

        val url: String = getString(R.string.mainUrl)
        val cookies: Map<String, String> = this.intent.getSerializableExtra("cookies") as Map<String, String>

        var startTimeDate = ""
        var startTimeMs = ""
        var endTimeMs = ""

        var ruleUrlRequestString =
            "rule?starttimems=$startTimeDate&starttime=$startTimeMs&endtime=$endTimeMs"

        val factor: Int = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 600)
        this.title = this.intent.getStringExtra("title")


        val startDate: TextView = findViewById(R.id.startDate)
        startDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val startTime: TextView = findViewById(R.id.startTime)
        val endTime: TextView = findViewById(R.id.endTime)
        val tempCheckBox: CheckBox = findViewById(R.id.checkBox)
        val tempMinTV: TextView = findViewById(R.id.minTempTV)
        val tempMaxTV: TextView = findViewById(R.id.maxTempTV)
        val tempMinET: EditText = findViewById(R.id.minTempET)
        val tempMaxET: EditText = findViewById(R.id.maxTempET)
        val tempViews: ArrayList<View> = ArrayList()

        tempViews.add(tempMinTV)
        tempViews.add(tempMinET)
        tempViews.add(tempMaxTV)
        tempViews.add(tempMaxET)

        val removeRule: Button = findViewById(R.id.removeCurrentFileDialogBtn)
        val cancelBtn: Button = findViewById(R.id.cancelDialogBtn)
        val okBtn: Button = findViewById(R.id.okDialogBtn)

        val currentTimeWithSeconds: String = Calendar.getInstance().time.toString().split(" ")[3]

        @Suppress("SpellCheckingInspection")
        var currTimeSplitted: List<String> = currentTimeWithSeconds.split(":")

        val currentTimeWithoutSeconds: String = currTimeSplitted[0] + ":" + currTimeSplitted[1]

        fun addOneHour(hour: String): Int {
            var addHour: Int = (hour.toInt() + 1)
            if (addHour >= 24) addHour = 0
            return addHour
        }

        val addHour: Int = addOneHour(currTimeSplitted[0])

        val currentTimeWithoutSeconds1: String

        fun fixTimeFormat(hour: String, minute: String): String {
            var minutes: String = minute
            if (minutes[0] != '0' && minutes.toInt() < 10) minutes = "0$minutes"

            return if (hour.toInt() < 10) "0$hour:$minutes"
            else "$hour:$minutes"
        }

        currentTimeWithoutSeconds1 = fixTimeFormat(addHour.toString(), currTimeSplitted[1])

        startTime.text = currentTimeWithoutSeconds
        endTime.text = currentTimeWithoutSeconds1

        startDate.setOnClickListener {
            // Get Current Date
            val c: Calendar = Calendar.getInstance()
            val year: Int = c.get(Calendar.YEAR)
            val month: Int = c.get(Calendar.MONTH)
            val day: Int = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog =
                DatePickerDialog(
                    this,
                    DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                        val dateString: String =
                            dayOfMonth.toString() + "." + (monthOfYear + 1) + "." + year
                        startDate.text = dateString
                    }, year, month, day
                )

            datePickerDialog.datePicker.minDate = c.timeInMillis // Past dates can't be chosen
            datePickerDialog.show()
        }

        fun timeOnClickListener(time: TextView) {
            var hours: Int
            val hour = 0
            val minutes = 0

            val timePickerDialog =
                TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { _, hour, minutes ->
                        time.text = fixTimeFormat(hour.toString(), minutes.toString())
                        hours = addOneHour(hour.toString())
                        if (time.id == R.id.startTime) endTime.text =
                            fixTimeFormat(hours.toString(), minutes.toString())
                    }, hour, minutes, true
                )

            currTimeSplitted = time.text.split(":")
            timePickerDialog.updateTime(currTimeSplitted[0].toInt(), currTimeSplitted[1].toInt())
            timePickerDialog.show()
        }

        startTime.setOnClickListener {
            timeOnClickListener(startTime)
        }

        endTime.setOnClickListener {
            timeOnClickListener(endTime)
        }

        removeRule.setOnClickListener {
            startTimeDate = "-1"
            startTimeMs = "-1"
            endTimeMs = "-1"

            ruleUrlRequestString =
                "rule?starttimems=$startTimeDate&starttime=$startTimeMs&endtime=$endTimeMs"

            println(ruleUrlRequestString)

            GlobalScope.launch(IO) {
                Jsoup.connect(url + ruleUrlRequestString)
                    .cookies(cookies)
                    .get()
            }
        }

        tempCheckBox.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            for (v: View in tempViews) {
                v.isEnabled = !v.isEnabled
            }
        }

        cancelBtn.setOnClickListener {
            //onBackPressed()
            this.finish()
        }

        okBtn.setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()
            val date: List<String> = startDate.text.split(".")
            val time: List<String> = startTime.text.split(":")
            calendar.set(date[2].toInt(), date[1].toInt(), date[0].toInt(), time[0].toInt(), time[1].toInt())
            startTimeDate = calendar.timeInMillis.toString()


            val endtime: List<String> = endTime.text.split(":")
            startTimeMs = (time[0].toInt() * 3600000 + time[1].toInt() * 60000).toString()
            endTimeMs = (endtime[0].toInt() * 3600000 + endtime[1].toInt() * 60000).toString()

            ruleUrlRequestString =
                "rule?starttimems=$startTimeDate&starttime=$startTimeMs&endtime=$endTimeMs"

            println(ruleUrlRequestString)

            GlobalScope.launch(IO) {
                Jsoup.connect(url + ruleUrlRequestString)
                    .cookies(cookies)
                    .get()
            }

            this.finish()
        }

    }


}
