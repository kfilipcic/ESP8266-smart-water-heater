package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.lang.Exception

class MainScreen : AppCompatActivity() , AdapterCallback {

    override suspend fun onMethodCallback(device: Device, url: String, cookies: Map<String, String>) {
        getDeviceDataFromServer(device, url, cookies)
    }

    private suspend fun getDeviceDataFromServer(device: Device, url: String, cookies: Map<String, String>) {
        val job = CoroutineScope(IO).launch {
            try {

                if (device is WaterHeater) {
                    val checkStateConnection =
                        Jsoup.connect(url + checkStateUrl)
                            .cookies(cookies)
                            .get()
                    val checkStatePage = JSONObject(checkStateConnection.body().text())

                    when (checkStatePage.get(deviceName).toString()) {
                        "0" -> device.state = "ON" // Yes, zero is for ON
                        "1" -> device.state = "OFF"
                    }
                }

                if (device is WaterHeater) {
                    val temperatureConnection =
                        Jsoup.connect(url + temperatureUrl)
                            .cookies(cookies)
                            .get()
                    val temperaturePage = JSONObject(temperatureConnection.body().text())

                    device.waterTemperature = temperaturePage.get(deviceName).toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error while getting device data from server!")
            }
        }
        job.join() // Wait for a response from the server
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val timeDateUrl: String = "entry.html"

    private var devicesList: MutableList<Device> = mutableListOf()

    private var deviceName: String = "device"
    private var checkStateUrl: String = "state"
    private var temperatureUrl: String = "temperature"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        prepareDeviceListData()

        val url: String = getString(R.string.mainUrl)
        val cookies: Map<String, String> =
            this.intent.getSerializableExtra("Map") as Map<String, String>

        val timeText: TextView = findViewById(R.id.timeText)
        val dateText: TextView = findViewById(R.id.dateText)


        @SuppressLint("SetTextI18n")
        fun timeDate() {
            CoroutineScope(IO).launch {
                val timeDatePage: Connection.Response = Jsoup.connect(url + timeDateUrl)
                    .cookies(cookies)
                    .execute()

                val doc: Document = timeDatePage.parse()

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

        viewAdapter = DevicesAdapter(this, devicesList, cookies, url)

        GlobalScope.launch(Dispatchers.Main) {
            for ((i: Int, device: Device) in devicesList.withIndex()) {
                if (device is WaterHeater) {
                    deviceName = "water_heater"
                    checkStateUrl = deviceName + "_" + (i + 1).toString() + "_" + "check" + ".html"
                    temperatureUrl =
                        deviceName + "_" + (i + 1).toString() + "_" + "temperature" + ".html"

                    getDeviceDataFromServer(device, url, cookies)

                }
            }
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
        val waterHeaterBathroom = WaterHeater("Water Heater Bathroom", "OFF", "N/A")
        val waterHeaterKitchen = WaterHeater("Water Heater Kitchen", "OFF", "N/A")

        devicesList.add(waterHeaterBathroom)
        devicesList.add(waterHeaterKitchen)
        devicesList.add(Heater("Kitchen heater"))
        devicesList.add(AirConditioner("Living room AC"))
        devicesList.add(AirConditioner("Main hall AC"))
    }

}

