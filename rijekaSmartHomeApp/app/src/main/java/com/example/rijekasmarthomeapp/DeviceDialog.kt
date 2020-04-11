package com.example.rijekasmarthomeapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class DeviceDialog : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_dialog)

        val factor:Int  = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 500)
        this.title = this.intent.getStringExtra("title")

        var startDate: TextView = findViewById(R.id.startDate)
        var startTime: TextView = findViewById(R.id.startTime)
        var endTime: TextView = findViewById(R.id.endTime)

        val currentTimeWithSeconds: String = Calendar.getInstance().time.toString().split(" ")[3]
        val currTimeSplitted: List<String> = currentTimeWithSeconds.split(":")
        val currentTimeWithoutSeconds: String = currTimeSplitted[0] + ":" + currTimeSplitted[1]
        var addHour: Int = (currTimeSplitted[0].toInt() + 1)
        if (addHour >= 24) addHour = 0;
        val currentTimeWithoutSeconds1: String
        currentTimeWithoutSeconds1 = if (addHour <= 10) "0" + addHour.toString() + ":" + currTimeSplitted[1]
        else addHour.toString() + ":" + currTimeSplitted[1]

        startTime.text = currentTimeWithoutSeconds
        endTime.text = currentTimeWithoutSeconds1


        startDate.setOnClickListener {
            val builder: MaterialDatePicker.Builder<*> = MaterialDatePicker.Builder.datePicker()
            val picker: MaterialDatePicker<*> = builder.build()
            picker.show(supportFragmentManager, picker.toString())
        }
    }


}
