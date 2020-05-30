package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GraphActivity : AppCompatActivity() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var cookieEditor: SharedPreferences.Editor
    private lateinit var cookie: String

    /*
    private lateinit var tempTempPref: SharedPreferences
    private lateinit var timeTempPref: SharedPreferences
    private lateinit var tempTempEditor: SharedPreferences.Editor
    private lateinit var timeTempEditor: SharedPreferences.Editor*/
    private var series = LineGraphSeries<DataPoint>()

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        /*
        tempTempPref = getSharedPreferences("tempTemp", 0)
        timeTempPref = getSharedPreferences("timeTemp", 0)
        */

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()

        cookiePreferences = applicationContext.getSharedPreferences("cookies", 0)
        cookieEditor = cookiePreferences.edit()

        cookie = cookiePreferences.getString("cookies", null).toString()
        /*
        tempTempEditor = tempTempPref.edit()
        timeTempEditor = timeTempPref.edit()
         */

        val position: Int = this.intent.getIntExtra("position", -1)
        val devicesList: MutableList<Device> = getDevicesList()
        val device: Device = devicesList[position]

        var deviceName = "default"
        var deviceTemp = "default"

        var prevDate: String = ""

        var timeSum: Long = 0
        var prevTimeMs: Long = 0
        var startedYet = false

        when (device) {
            is WaterHeater -> {
                deviceName = "water_heater"
                deviceTemp = device.temperature
            }
            is Heater -> {
                deviceName = "heater"
                deviceTemp = device.temperature
            }
            is AirConditioner -> {
                deviceName = "ac"
                deviceTemp = device.temperature
            }
        }

        val graph = findViewById<GraphView>(R.id.graph_view)
        val db = dataBaseHelpter(this)

        val rs = db.getData(deviceName + "_" + device.id_num.toString())

        rs.use { rs ->
            while (rs.moveToNext()) {
                val timeMs = rs.getLong(rs.getColumnIndex("time"))
                if (rs.getString(rs.getColumnIndex("temperature")) != "") {
                    if (!startedYet) {
                        prevTimeMs = timeMs
                        startedYet = true
                    } else {
                        timeSum += (timeMs-prevTimeMs)
                        prevTimeMs = timeMs
                    }
                    val temp: Double = rs.getDouble(rs.getColumnIndex("temperature"))
                    series.appendData(DataPoint(Date(timeMs), temp), true, 1000)
                } else {
                    startedYet = false
                    series.appendData(DataPoint(Date(timeMs), Double.NaN), true, 1000)
                }
            }
            graph.addSeries(series)
        }

        /*
        tempTempEditor.putString("tempTemp", null)
        timeTempEditor.putString("timeTemp", null)
        tempTempEditor.apply()
        timeTempEditor.apply()
        */


        // set date label formatter
        graph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (isValueX) {
                    var formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    var date = formatter.format(Date(value.toLong()))
                    var dateToShow =
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(value.toLong()))
                    if (prevDate == "") {
                        prevDate = date
                        dateToShow =
                            SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.getDefault()).format(
                                Date(value.toLong())
                            )
                    } else if (date != prevDate) {
                        prevDate = date
                        dateToShow =
                            SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.getDefault()).format(
                                Date(value.toLong())
                            )
                    }
                    return dateToShow
                } else {
                    return super.formatLabel(value, isValueX)
                }
            }
        }

        graph.viewport.isScalable = true
        graph.gridLabelRenderer.verticalAxisTitle = "Temperature (°C)"
        graph.gridLabelRenderer.horizontalAxisTitle = "\n\nTime"
        graph.gridLabelRenderer.padding = 10
        graph.viewport.isScrollable = true

        val avgTempCursor = db.getAvgTempData(deviceName + "_" + device.id_num.toString())

        // Get and set the average temperature
        avgTempCursor.use { avgTempCursor ->
            avgTempCursor.moveToFirst()
            val averageTemperature = avgTempCursor.getString(0)
            val avgTempTV = findViewById<TextView>(R.id.avgTempTV)
            avgTempTV.text = getString(R.string.average_temp_tv, averageTemperature)
            title = device.name
        }

        // Set the total on time
        val totalTimeTV = findViewById<TextView>(R.id.totalTimeTV)

        val days = TimeUnit.MILLISECONDS.toDays(timeSum)
        timeSum -= TimeUnit.DAYS.toMillis(days)
        val hours = TimeUnit.MILLISECONDS.toHours(timeSum)
        timeSum -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeSum)
        timeSum -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeSum)

        val timeFormat = String.format("%d days, %d hours, %d minutes and %d seconds", days, hours, minutes, seconds)
        totalTimeTV.text = getString(R.string.total_time_tv, timeFormat)

        // Show the current device temperature
        val currTempTV = findViewById<TextView>(R.id.currentTempTV)
        currTempTV.text = getString(R.string.curr_temp_tv, deviceTemp)
    }

    private fun getDevicesList(): MutableList<Device> {
        var arrayItems: MutableList<Device> = mutableListOf()
        val serializedObject: String? =
            devicesListPreferences.getString("devicesList", null)
        if (serializedObject != null) {
            val gson: Gson = GsonBuilder().registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory
                    .of(Device::class.java)
                    .registerSubtype(WaterHeater::class.java)
                    .registerSubtype(Heater::class.java)
                    .registerSubtype(AirConditioner::class.java)
                        as RuntimeTypeAdapterFactory<Device>
            ).create()
            val type: Type =
                TypeToken.getParameterized(MutableList::class.java, Device::class.java).type
            arrayItems = gson.fromJson(serializedObject, type)
        }
        return arrayItems
    }

    fun setDevicesList(key: String, list: MutableList<Device>) {
        val gson: Gson = GsonBuilder().registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory
                .of(Device::class.java)
                .registerSubtype(WaterHeater::class.java)
                .registerSubtype(Heater::class.java)
                .registerSubtype(AirConditioner::class.java)
                    as RuntimeTypeAdapterFactory<Device>
        ).create()
        val type = TypeToken.getParameterized(MutableList::class.java, Device::class.java).type
        val json: String = gson.toJson(list, type)

        set(key, json)
    }

    fun set(key: String, value: String) {
        editor.putString(key, value)
        editor.commit()
    }
    /*
    override fun onResume() {
        super.onResume()
        val timer = object : Runnable {
            public override fun run() {
                println("fun run")
                val newTime = timeTempPref.getString("timeTemp", null)
                val newTemp= tempTempPref.getString("tempTemp", null)

                if (newTemp != null && newTime != null) {
                    println("ue")
                    runOnUiThread {
                        series.appendData(DataPoint(Date(newTime.toLong()), newTemp.toDouble()), true, 1000)
                    }
                    tempTempEditor.putString("tempTemp", null)
                    tempTempEditor.apply()
                    timeTempEditor.putString("timeTemp", null)
                    timeTempEditor.apply()

                }
            }
        }
        timer.run()
    }*/
}
