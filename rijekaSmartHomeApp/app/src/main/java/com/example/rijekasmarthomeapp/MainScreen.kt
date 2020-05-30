package com.example.rijekasmarthomeapp

import android.R.attr.data
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.lang.reflect.Type


@Suppress("UNCHECKED_CAST")
class MainScreen : AppCompatActivity(), AdapterCallback {
    override suspend fun onMethodCallback(
        device: Device,
        url: String
    ) {
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var cookieEditor: SharedPreferences.Editor

    private val timeDateUrl: String = "entry.html"

    private var devicesList: MutableList<Device> = mutableListOf()

    private var deviceName: String = "default"

    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var logPreferences: SharedPreferences
    private lateinit var logEditor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        logPreferences = applicationContext.getSharedPreferences("logStats", 0)

        editor = devicesListPreferences.edit()
        logEditor = logPreferences.edit()

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()
        cookiePreferences = applicationContext.getSharedPreferences("cookies", 0)
        cookieEditor = cookiePreferences.edit()

        val cookie = cookiePreferences.getString("cookies", null)

        if (getDevicesList().size < 1) prepareDeviceListData()

        //val cookies: Map<String, String> =
         //   this.intent.getSerializableExtra("Map") as Map<String, String>

        val timeText: TextView = findViewById(R.id.timeText)
        val dateText: TextView = findViewById(R.id.dateText)


        fun timeDate() {
            CoroutineScope(IO).launch {
                val doc: Document = Jsoup.connect(Secrets().url + timeDateUrl)
                    //.cookies(cookies)
                    .cookie("ARDUINOSESSIONID", cookie)
                    .timeout(60000)
                    .get()

                val timeDateElements: Elements = doc.select("h1")

                val timeDateString = arrayListOf<String>()

                timeDateElements.forEach { element: Element ->
                    timeDateString.add(element.text().toString())
                }

                // Shows the server time - needs to be in this method
                // because it changes the UI during runtime
                runOnUiThread {
                    try {
                        dateText.text = timeDateString[1].replace("Date: ", "")
                        timeText.text = timeDateString[0].replace("Time: ", "")
                    } catch (e: IndexOutOfBoundsException) {
                       System.err.println("dateText and/or timeText is out of bounds. Possibly because the session is timed out and it is not receiving data. Returning to login screen...")
                        e.printStackTrace()
                        // Go back to login screen
                        finish()
                        val loginIntent = Intent(applicationContext, MainActivity::class.java)
                            .putExtra("errorCode", -1)
                        startActivity(loginIntent)
                    }

                    // Show single digit numbers as double digits (using regex)
                    // Example: 03:30:05 instead of 3:30:5
                    // Only for time
                    val regex = "(\\d+):(\\d+):(\\d+)".toRegex()
                    val match = regex.find(timeText.text)

                    if (match != null) {
                        var h: String = match.groupValues[1]
                        var m: String = match.groupValues[2]
                        var s: String = match.groupValues[3]

                        for (i: Int in match.groupValues.indices) {
                            if (i > 0 && match.groupValues[i].length < 2) {
                                when (i) {
                                    1 -> h = "0$h"
                                    2 -> m = "0$m"
                                    3 -> s = "0$s"
                                }
                            }
                        }

                        timeText.text = "$h:$m:$s"
                    }
                }
            }
        }


        timeDate()

        viewAdapter = DevicesAdapter(this, Secrets().url)

        GlobalScope.launch(Dispatchers.Main) {
            viewManager = LinearLayoutManager(parent)
            recyclerView = findViewById<RecyclerView>(R.id.devices_list).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }

        // Alarm for getting log data
        val logBroadcastIntent = Intent(this, LogAlarmReceiver::class.java)
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val logPendingIntent = PendingIntent.getBroadcast(this, 1, logBroadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (logPendingIntent != null && alarmManager != null) {
            alarmManager.cancel(logPendingIntent)
        }

        alarmManager?.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime(),
            SystemClock.elapsedRealtime() + 300,
            logPendingIntent
        )
        /*
        alarmManager?.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            logPendingIntent
        )*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.refreshActivityButton) {
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun prepareDeviceListData() {
        val waterHeaterBathroom = WaterHeater("Water Heater Bathroom", "OFF", "N/A", 1)
        val waterHeaterKitchen = WaterHeater("Water Heater Kitchen", "OFF", "N/A", 2)

        devicesList.add(waterHeaterBathroom)
        devicesList.add(waterHeaterKitchen)
        devicesList.add(Heater("Kitchen heater", 1))
        devicesList.add(AirConditioner("Living room AC", 1))
        devicesList.add(AirConditioner("Main hall AC", 2))

        setDevicesList("devicesList", devicesList)
    }

    fun getDevicesList(): MutableList<Device> {
        var arrayItems: MutableList<Device> = mutableListOf()
        val serializedObject: String? =
            devicesListPreferences.getString("devicesList", null)
        if (serializedObject != null) {
            var gson: Gson = GsonBuilder().registerTypeAdapterFactory(
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

    fun getLogList(): Map<String, ArrayList<Pair<Double, Long>>> {
        var logList: Map<String, ArrayList<Pair<Double, Long>>>
        val gson = Gson()
        return gson.fromJson(logPreferences.getString("logStats", null), object : TypeToken<Map<String, ArrayList<Pair<Double, Long>>>>(){}.type)
    }


    fun setDevicesList(key: String, list: MutableList<Device>) {
        var gson: Gson = GsonBuilder().registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory
                .of(Device::class.java)
                .registerSubtype(WaterHeater::class.java)
                .registerSubtype(Heater::class.java)
                .registerSubtype(AirConditioner::class.java)
                    as RuntimeTypeAdapterFactory<Device>
        ).create()
        val type = TypeToken.getParameterized(MutableList::class.java, Device::class.java).type
        val json: String = gson.toJson(list, type)

        set(key, json, editor)
    }

    fun setLogList(key: String, list: Map<String, ArrayList<Pair<Double, Long>>>) {
        val gson = Gson()
        val logList: Map<String, ArrayList<Pair<Double, Long>>> = mapOf()
        //
        val jsonText = gson.toJson(logList)
        logEditor.putString("logStats", jsonText)
        logEditor.apply()
    }

    fun set(key: String, value: String, editor: SharedPreferences.Editor) {
        editor.putString(key, value)
        editor.commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewAdapter.notifyDataSetChanged()
    }
}

