package com.example.rijekasmarthomeapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.rijekasmarthomeapp.R.string.waterHeater1
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class MainScreen : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var devicesList: MutableList<Device> = mutableListOf<Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        viewManager = LinearLayoutManager(this)
        viewAdapter = DevicesAdapter(devicesList)

        prepareDeviceListData()

        recyclerView = findViewById<RecyclerView>(R.id.devices_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        val waterHeaterCheckUrl: String = "water_heater_check.html"
        val waterHeaterSwitchUrl: String = "water_heater_switch.html"
        val updateTimeUrl: String = "updateTime.html"
        val timeDateUrl: String = "entry.html"

        val url: String = getString(R.string.mainUrl)
        val cookies: Map<String, String> =
            this.intent.getSerializableExtra("Map") as Map<String, String>

        val toggleWaterHeaterButton: ImageButton = findViewById(R.id.toggleWaterHeaterButton)
        val timeText: TextView = findViewById(R.id.timeText)
        val dateText: TextView = findViewById(R.id.dateText)


        fun timeDate() {
            CoroutineScope(IO).launch {
               val timeDatePage: Connection.Response = Jsoup.connect(url + timeDateUrl)
                   .cookies(cookies)
                   .execute()

                val doc : Document = timeDatePage.parse()

                val timeDateElements :Elements = doc.select("h1")

               val timeDateString = arrayListOf<String>()

                timeDateElements.forEach { element :Element ->
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
                    var regex = "(\\d+):(\\d+):(\\d+)".toRegex()
                    var match = regex.find(timeText.text)

                    if (match != null) {
                        var h: String = match.groupValues[1]
                        var m: String = match.groupValues[2]
                        var s: String = match.groupValues[3]

                        println(match.groupValues.size)

                        for (i: Int in  0..match.groupValues.size-1) {
                            if (i > 0 && match.groupValues[i].length < 2) {
                                when(i) {
                                    1 -> h = "0" + h
                                    2 -> m = "0" + m
                                    3 -> s = "0" + s
                                }
                            }
                        }

                        timeText.text = h + ":" + m + ":" + s
                    }
                }
            }
        }

        fun waterHeater(waterHeaterUrl:String) {
            CoroutineScope(IO).launch {
                val waterHeaterPage: Connection.Response = Jsoup.connect(url + waterHeaterUrl)
                    .cookies(cookies)
                    .execute()

                val waterHeater = JSONObject(waterHeaterPage.body())
                val waterHeaterState = waterHeater.get("water_heater").toString()

                if (waterHeaterState == "1") {
                    toggleWaterHeaterButton.setImageResource(R.drawable.boiler_off)
                } else {
                    toggleWaterHeaterButton.setImageResource(R.drawable.boiler_on)
                }
            }
        }

        timeDate()
        // Upon starting the activity, check the water heater
        // state for the appropriate selection of the button image
        waterHeater(waterHeaterCheckUrl)

        toggleWaterHeaterButton.setOnClickListener {
            waterHeater(waterHeaterSwitchUrl)
        }

        /*
        toggleWaterHeaterButton.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        picker.show(supportFragmentManager, picker.toString())
                    }
                }
                return v?.onTouchEvent(event) ?: true
            }
        })*/


        val intent = Intent(this, DeviceDialog::class.java)

        toggleWaterHeaterButton.setOnLongClickListener (object: View.OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                startActivity(intent.putExtra("title", getString(waterHeater1)))
                return false
            }
        })

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
        val waterHeater: Device = Device("waterHeater1", false)//, "50.5", "20.5")
        System.out.println(devicesList)
        devicesList.add(waterHeater)
        devicesList.add(waterHeater)
        devicesList.add(waterHeater)
        System.out.println(devicesList)
    }

}

