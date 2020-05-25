package com.example.rijekasmarthomeapp

import android.content.SharedPreferences
import android.os.Bundle
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
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
class MainScreen : AppCompatActivity(), AdapterCallback {
    override suspend fun onMethodCallback(
        device: Device,
        url: String,
        cookies: Map<String, String>
    ) {
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val timeDateUrl: String = "entry.html"

    private var devicesList: MutableList<Device> = mutableListOf()

    private var deviceName: String = "default"

    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()

        if (getDevicesList().size < 1) prepareDeviceListData()

        val url: String = getString(R.string.mainUrl)
        val cookies: Map<String, String> =
            this.intent.getSerializableExtra("Map") as Map<String, String>

        val timeText: TextView = findViewById(R.id.timeText)
        val dateText: TextView = findViewById(R.id.dateText)


        fun timeDate() {
            CoroutineScope(IO).launch {
                val doc: Document = Jsoup.connect(url + timeDateUrl)
                    .cookies(cookies)
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
                    dateText.text = timeDateString[1].replace("Date: ", "")
                    timeText.text = timeDateString[0].replace("Time: ", "")

                    // Show single digit numbers as double digits (using regex)
                    // Example: 03:30:05 instead of 3:30:5
                    // Only for time
                    val regex = "(\\d+):(\\d+):(\\d+)".toRegex()
                    val match = regex.find(timeText.text)

                    if (match != null) {
                        var h: String = match.groupValues[1]
                        var m: String = match.groupValues[2]
                        var s: String = match.groupValues[3]

                        println(match.groupValues.size)

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

        viewAdapter = DevicesAdapter(this, cookies, url)

        GlobalScope.launch(Dispatchers.Main) {
            viewManager = LinearLayoutManager(parent)
            recyclerView = findViewById<RecyclerView>(R.id.devices_list).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }
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

        set(key, json)
    }

    fun set(key: String, value: String) {
        editor.putString(key, value)
        editor.commit()
    }
}

