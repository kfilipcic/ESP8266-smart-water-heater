package com.example.rijekasmarthomeapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.lang.ClassCastException
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import kotlin.properties.Delegates


class ACRemoteDialog : AppCompatActivity() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var devicesList: MutableList<Device>
    private lateinit var device: Device
    private lateinit var cookies: Map<String, String>
    private var position by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factor: Int = this.resources.displayMetrics.density.toInt()
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, factor * 500)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        setContentView(R.layout.ac_remote_dialog)

        devicesListPreferences = applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()

        cookies =
            this.intent.getSerializableExtra("cookies") as Map<String, String>

        val powerButton: ImageButton = findViewById(R.id.powerButtonAC)
        position = this.intent.getIntExtra("position", -1)
        devicesList = getDevicesList()
        device = devicesList[position]

        powerButton.setOnClickListener {

            try {
                GlobalScope.launch(Dispatchers.IO) {
                    var switchUrl = "default"
                    when (device) {
                        is WaterHeater -> switchUrl =
                            "water_heater_" + device.id_num.toString() + "_switch.html"
                        is Heater -> switchUrl =
                            "heater_" + device.id_num.toString() + "_switch.html"
                        is AirConditioner -> switchUrl =
                            "ac_" + device.id_num.toString() + "_switch.html"
                        }
                    val url = getString(R.string.mainUrl)
                    Jsoup.connect(url + switchUrl)
                        .cookies(cookies)
                        .get()
                    withContext(Dispatchers.Main) {
                        try {

                        } catch (e: ClassCastException) {
                            e.printStackTrace()
                        }
                    }
                }
        } catch (e: SocketTimeoutException) {
                System.err.println("Session expired! Returning to the login screen...")
                val loginIntent: Intent = Intent(applicationContext, MainActivity::class.java)
                    .putExtra("errorCode", -1)
                applicationContext.startActivity(loginIntent)

                e.printStackTrace()
            } catch (e: java.lang.Exception) {
                System.err.println("Connecting with server and/or calling method MainScreen.getDataFromServer() unsuccessful")
                e.printStackTrace()
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

        fun getStateFromServer(
            deviceName: String,
            checkStateUrl: String,
        url: String
        ) {
            try {
                val checkStateConnection =
                    Jsoup.connect(url + checkStateUrl)
                        .cookies(cookies)
                        .get()
                val checkStatePage = JSONObject(checkStateConnection.body().text())

                when (checkStatePage.get(deviceName).toString()) {
                    "0" -> device.state = "ON" // Yes, zero is for ON
                    "1" -> device.state = "OFF"
                }

            } catch (e: Exception) {
                System.err.println("Error getting device state data from the server!")
                System.err.println(url + checkStateUrl)
                e.printStackTrace()
            }
        }
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