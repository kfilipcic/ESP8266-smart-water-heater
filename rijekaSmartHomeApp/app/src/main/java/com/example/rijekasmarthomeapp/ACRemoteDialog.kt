package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.w3c.dom.Text
import java.lang.ClassCastException
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import kotlin.properties.Delegates


class ACRemoteDialog : AppCompatActivity() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var devicesList: MutableList<Device>
    private lateinit var device: AirConditioner
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var cookieEditor: SharedPreferences.Editor
    private lateinit var cookie: String

    private var position by Delegates.notNull<Int>()
    private val modeStrings: ArrayList<String> = arrayListOf("AUTO", "COOL", "DRY", "HEAT", "FAN")
    private val maxRemoteTemp = 30
    private val minRemoteTemp = 17

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factor: Int = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 500)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        setContentView(R.layout.ac_remote_dialog)

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()

        cookiePreferences = applicationContext.getSharedPreferences("cookies", 0)
        cookieEditor = cookiePreferences.edit()

        cookie = cookiePreferences.getString("cookies", null).toString()
        //cookies = this.intent.getSerializableExtra("cookies") as Map<String, String>

        // Buttons
        val powerButton: ImageButton = findViewById(R.id.powerButtonAC)
        val modeButton: ImageButton = findViewById(R.id.modeBtn)
        val swingButton: ImageButton = findViewById(R.id.swingBtn)
        val sleepButton: ImageButton = findViewById(R.id.sleepBtn)
        val directButton: ImageButton = findViewById(R.id.directBtn)
        val tempUpButton: ImageButton = findViewById(R.id.tempUpBtn)
        val tempDownButton: ImageButton = findViewById(R.id.tempDownBtn)
        val fanButton: ImageButton = findViewById(R.id.fanBtn)

        position = this.intent.getIntExtra("position", -1)
        devicesList = getDevicesList()
        device = devicesList[position] as AirConditioner


        // Screen TextViews
        var runTV: TextView = findViewById(R.id.runTV)
        var modeTV: TextView = findViewById(R.id.modeTV)
        var swingTV: TextView = findViewById(R.id.swingTV)
        var sleepTV: TextView = findViewById(R.id.sleepTV)
        var directTV: TextView = findViewById(R.id.directTV)
        var fanTV: TextView = findViewById(R.id.fanTV)
        var fanTextView: TextView = findViewById(R.id.fanTextView)
        var tempTV: TextView = findViewById(R.id.acTemperatureTV)

        when (device.state) {
            "OFF" -> runTV.visibility = View.INVISIBLE
            "ON" -> runTV.visibility = View.VISIBLE
        }

        if (device.sleep) sleepTV.visibility = View.VISIBLE
        else sleepTV.visibility = View.INVISIBLE
        if (device.swing) swingTV.visibility = View.VISIBLE
        else swingTV.visibility = View.INVISIBLE
        if (device.direct) directTV.visibility = View.VISIBLE
        else directTV.visibility = View.INVISIBLE

        if (device.mode < 0) device.mode = 0
        if (device.mode > -1) {
            when (device.mode) {
                0 -> {
                    // This mode does not have a fan function
                    modeTV.text = modeStrings[0]
                    fanTV.visibility = View.INVISIBLE
                    fanTextView.visibility = View.INVISIBLE
                    fanButton.isEnabled = false
                }
                1 -> modeTV.text = modeStrings[1]
                2 -> {
                    // This mode does not have a fan function
                    modeTV.text = modeStrings[2]
                    fanTV.visibility = View.INVISIBLE
                    fanTextView.visibility = View.INVISIBLE
                    fanButton.isEnabled = false
                }
                3 -> modeTV.text = modeStrings[3]
                4 -> modeTV.text = modeStrings[4]
            }
        }

        if (fanButton.isEnabled) {
            if (device.fanLevel > -1) {
                fanTV.text = device.fanLevel.toString() + "/3"
            } else {
                device.fanLevel = 0
            }
        }

        if (device.mode == 4) {
            tempDownButton.isEnabled = false
            tempUpButton.isEnabled = false
            tempTV.visibility = View.INVISIBLE
        } else {
            tempDownButton.isEnabled = true
            tempUpButton.isEnabled = true
            tempTV.visibility = View.VISIBLE
        }

        if (device.tempLevel > 0) {
            tempTV.text = device.tempLevel.toString() + "°C"
        }

        powerButton.setOnClickListener {
            var deviceName = "ac"

            val switchUrl =
                "ac_" + device.id_num.toString() + "_switch.html"

            var stateVal = true

            GlobalScope.launch(Dispatchers.Main) {
                stateVal = switchOnServer(deviceName, switchUrl, Secrets().url)

                when (stateVal) {
                    false -> device.state = "ON"
                    true -> device.state = "OFF"
                }

                when (device.state) {
                    "OFF" -> {
                        runTV.visibility = View.INVISIBLE
                    }
                    "ON" -> {
                        runTV.visibility = View.VISIBLE
                    }
                }
                devicesList[position] = device
                setDevicesList("devicesList", devicesList)
            }

            /*
            val root: RelativeLayout = findViewById(R.id.relativeLayoutACDialog)
            val ib: ImageButton = findViewById(R.id.powerButtonAC)

            val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(150, 150)
            params.leftMargin = 50
            params.topMargin = 900
            root.removeView(ib)
            root.addView(ib, params)*/

        }

        swingButton.setOnClickListener {
            //device.swing = !device.swing

            val deviceName = "ac_" + device.id_num.toString()
            GlobalScope.launch(Dispatchers.Main) {
                device.swing =
                    switchOnServer("ac", deviceName + "_swing_switch.html", Secrets().url)

                if (device.swing) swingTV.visibility = View.VISIBLE
                else swingTV.visibility = View.INVISIBLE

                devicesList[position] = device
                setDevicesList("devicesList", devicesList)
            }
        }

        sleepButton.setOnClickListener {
            //device.sleep = !device.sleep

            val deviceName = "ac_" + device.id_num.toString()

            GlobalScope.launch(Dispatchers.Main) {
                device.sleep =
                    switchOnServer("ac", deviceName + "_sleep_switch.html", Secrets().url)
                if (device.sleep) sleepTV.visibility = View.VISIBLE
                else sleepTV.visibility = View.INVISIBLE

                devicesList[position] = device
                setDevicesList("devicesList", devicesList)
            }
        }

        directButton.setOnClickListener {
            device.direct = !device.direct

            val deviceName = "ac_" + device.id_num.toString()

            GlobalScope.launch(Dispatchers.Main) {
                device.direct =
                    switchOnServer("ac", deviceName + "_sleep_switch.html", Secrets().url)
                if (device.direct) directTV.visibility = View.VISIBLE
                else directTV.visibility = View.INVISIBLE

                devicesList[position] = device
                setDevicesList("devicesList", devicesList)
            }
        }

        modeButton.setOnClickListener {
            println("pajbt: " + device.mode)
            if (device.mode > -1) {
                GlobalScope.launch(Dispatchers.Main) {
                    device.mode = switchModeOrFan(
                        Secrets().url,
                        "ac_" + device.id_num.toString() + "_mode_switch.html",
                        "ac"
                    )
                    when (device.mode) {
                        0 -> {
                            modeTV.text = modeStrings[0]
                            //device.mode = 1
                            fanTV.visibility = View.INVISIBLE
                            fanTextView.visibility = View.INVISIBLE
                            fanButton.isEnabled = false

                            tempDownButton.isEnabled = true
                            tempUpButton.isEnabled = true
                            tempTV.visibility = View.VISIBLE

                        }
                        1 -> {
                            modeTV.text = modeStrings[1]
                            //device.mode = 2
                            fanTV.visibility = View.VISIBLE
                            fanTextView.visibility = View.VISIBLE
                            fanButton.isEnabled = true

                        }
                        2 -> {
                            modeTV.text = modeStrings[2]
                            //device.mode = 3
                            fanTV.visibility = View.INVISIBLE
                            fanTextView.visibility = View.INVISIBLE
                            fanButton.isEnabled = false

                        }
                        3 -> {
                            modeTV.text = modeStrings[3]
                            //device.mode = 4
                            fanTV.visibility = View.VISIBLE
                            fanTextView.visibility = View.VISIBLE
                            fanButton.isEnabled = true

                        }
                        4 -> {
                            modeTV.text = modeStrings[4]
                            //device.mode = 0

                            fanTV.visibility = View.VISIBLE
                            fanTextView.visibility = View.VISIBLE
                            fanButton.isEnabled = true

                            tempDownButton.isEnabled = false
                            tempUpButton.isEnabled = false
                            tempTV.visibility = View.INVISIBLE
                        }
                    }

                    devicesList[position] = device
                    setDevicesList("devicesList", devicesList)
                }
            }
        }

        fanButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                device.fanLevel = switchModeOrFan(
                    Secrets().url,
                    "ac_" + device.id_num.toString() + "_fan_switch.html",
                    "ac"
                )
                //device.fanLevel = ++device.fanLevel % 4
                fanTV.text = device.fanLevel.toString() + "/3"

                devicesList[position] = device
                setDevicesList("devicesList", devicesList)
            }
        }

        tempDownButton.setOnClickListener {
            // First check the temperature
            GlobalScope.launch(Dispatchers.Main) {
                device.tempLevel = switchTempLevelOnServer(
                    Secrets().url,
                    "ac_" + device.id_num.toString() + "_temp_check.html",
                    "ac"
                )
                if (device.tempLevel > 17) {
                    device.tempLevel = switchTempLevelOnServer(
                        Secrets().url,
                        "ac_" + device.id_num.toString() + "_temp_down.html",
                        "ac"
                    )
                    //device.tempLevel--
                    tempTV.text = device.tempLevel.toString() + "°C"

                    devicesList[position] = device
                    setDevicesList("devicesList", devicesList)
                }
            }
        }

        tempUpButton.setOnClickListener {
            // First check the temperature
            GlobalScope.launch(Dispatchers.Main) {
                device.tempLevel = switchTempLevelOnServer(
                    Secrets().url,
                    "ac_" + device.id_num.toString() + "_temp_check.html",
                    "ac"
                )
                if (device.tempLevel < 30) {
                    device.tempLevel = switchTempLevelOnServer(
                        Secrets().url,
                        "ac_" + device.id_num.toString() + "_temp_up.html",
                        "ac"
                    )
                    //device.tempLevel++
                    tempTV.text = device.tempLevel.toString() + "°C"

                    devicesList[position] = device
                    setDevicesList("devicesList", devicesList)
                }
            }
        }

    }

    suspend fun switchOnServer(
        deviceName: String,
        checkStateUrl: String,
        url: String
    ): Boolean {
        var valueToSwitch = false
        try {
            val checkStateConnection = withContext(Dispatchers.IO) {
                Jsoup.connect(url + checkStateUrl)
                    //.cookies(cookies)
                    .cookie("ARDUINOSESSIONID", cookie)
                    .get()
            }
            val checkStatePage = JSONObject(checkStateConnection.body().text())

            when (checkStatePage.get(deviceName).toString()) {
                "0" -> valueToSwitch = true // Yes, zero is for ON
                "1" -> valueToSwitch = false
            }

        } catch (e: Exception) {
            System.err.println("Error getting device state data from the server!")
            System.err.println(url + checkStateUrl)
            e.printStackTrace()
        }
        return valueToSwitch
    }

    suspend fun switchModeOrFan(url: String, checkStateUrl: String, deviceName: String): Int {
        var valueToSwitch = 0
        try {
            val checkStateConnection = withContext(Dispatchers.IO) {
                Jsoup.connect(url + checkStateUrl)
                    .cookie("ARDUINOSESSIONID", cookie)
                    .timeout(10 * 1000)
                    .get()
            }
            val checkStatePage = JSONObject(checkStateConnection.body().text())

            valueToSwitch = checkStatePage.get(deviceName).toString().toInt()

        } catch (e: Exception) {
            System.err.println("Error getting device state data from the server!")
            System.err.println(url + checkStateUrl)
            e.printStackTrace()
        }
        return valueToSwitch
    }

    suspend fun switchTempLevelOnServer(
        url: String,
        checkStateUrl: String,
        deviceName: String
    ): Int {
        var valueToSwitch = 0
        try {
            val checkStateConnection = withContext(Dispatchers.IO) {
                Jsoup.connect(url + checkStateUrl)
                    .cookie("ARDUINOSESSIONID", cookie)
                    .get()
            }
            val checkStatePage = JSONObject(checkStateConnection.body().text())

            valueToSwitch = checkStatePage.get(deviceName).toString().toInt()

        } catch (e: Exception) {
            System.err.println("Error getting device state data from the server!")
            System.err.println(url + checkStateUrl)
            e.printStackTrace()
        }
        return valueToSwitch
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


    fun set(key: String, value: String) {
        editor.putString(key, value)
        editor.commit()
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

}