package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.Serializable
import java.lang.reflect.Type
import java.util.*

@Suppress(
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "UNCHECKED_CAST", "unused"
)
class CheckTemperatureAlarmReceiver : BroadcastReceiver() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var cookieEditor: SharedPreferences.Editor
    private lateinit var cookie: String

    @SuppressLint("CommitPrefEdits")
    override fun onReceive(context: Context, intent: Intent) {
        devicesListPreferences = context.applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()
        cookiePreferences = context.applicationContext.getSharedPreferences("cookies", 0)
        cookieEditor = cookiePreferences.edit()

        cookie = cookiePreferences.getString("cookies", null).toString()

        val devicesList: MutableList<Device> = getDevicesList()
        var notificationIconId = 0

        try {
            GlobalScope.launch(IO) {
                var type = ""
                var minTemp = 0.0
                var maxTemp = 0.0
                var autoRegTemp = false
                var id_ext = 0

                for (device: Device in devicesList) {
                    when (device) {
                        is WaterHeater -> {
                            type = "water_heater"
                            minTemp = device.minTemp
                            maxTemp = device.maxTemp
                            autoRegTemp = device.autoRegulateTemperature
                            notificationIconId = R.drawable.boiler_noinfo
                        }
                        is Heater -> {
                            type = "heater"
                            minTemp = device.minTemp
                            maxTemp = device.maxTemp
                            autoRegTemp = device.autoRegulateTemperature
                            id_ext = 1
                            notificationIconId = R.drawable.heater_noinfo
                        }
                        is AirConditioner -> {
                            type = "ac"
                            minTemp = device.minTemp
                            maxTemp = device.maxTemp
                            autoRegTemp = device.autoRegulateTemperature
                            id_ext = 2
                            notificationIconId = R.drawable.ac_off
                        }
                    }
                    val tempUrl =
                        Secrets().url + type + "_" + device.id_num.toString() + "_" + "temperature.html"

                    val temperatureHTML =
                        Jsoup.connect(tempUrl)
                            .cookie("ARDUINOSESSIONID", cookie)
                            .get()
                    val temperaturePage = JSONObject(temperatureHTML.body().text())
                    val currentTemp: Double =
                        temperaturePage.get(type).toString().toDouble()

                    println("ocelito")
                    if (currentTemp < minTemp || currentTemp > maxTemp) {
                        println("nece")
                        // Don't show notifications if the device device is already working on going back to it's range values
                        println(autoRegTemp)
                        println(currentTemp)
                        println(minTemp)
                        println(maxTemp)
                        println(device.state)


                        if (autoRegTemp && ((currentTemp < minTemp && device.state == "ON") || (currentTemp > maxTemp && device.state == "OFF"))) return@launch
                        println("iloce")
                        //println("minTemp: $minTemp")
                        //println("maxTemp: $maxTemp")
                        //println("currentTemp$currentTemp")

                        //val cookies: MutableMap<String, String> = mutableMapOf<String, String>()

                        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
                        val notificationUtils = NotificationUtils(context)
                        // val notificationDetails = intent.getStringArrayListExtra("notif")

                        var notificationTitle: String =
                            "Turning on " + device.name + "..."
                        if (autoRegTemp) {
                            // Switch state
                            val state = switchOnServer(
                                type,
                                type + "_" + device.id_num.toString() + "_switch.html",
                                Secrets().url
                            )
                            when (state) {
                                false -> device.state = "ON"
                                true -> device.state = "OFF"
                            }
                            if (currentTemp > maxTemp) {
                                notificationTitle = "Turning off " + device.name + "..."
                            }
                        } else {
                            notificationTitle = "Temperature not in range!"
                        }
                        val notificationDescription: String =
                            device.name + " is currently at " + currentTemp + "°C"

                        val notification = notificationUtils.getNotificationBuilder(
                            notificationTitle,
                            notificationDescription,
                            notificationIconId
                        ).build()

                        notificationUtils.getManager().notify(device.id_num + id_ext * 10, notification)
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error while trying to create temperature notification. Trying again with new cookies...")
            try {
                // Create new session by logging in again and then try running this AlarmReceiver again
                login(Secrets().url, Secrets().username, Secrets().password)
                this.onReceive(context, intent)
            } catch (e: Exception) {
                System.err.println("Logging in again also doesn't solve the problem... rip")
                e.printStackTrace()
            }

            e.printStackTrace()
        }

    }

    suspend fun switchOnServer(
        deviceName: String,
        checkStateUrl: String,
        url: String
    ): Boolean {
        var valueToSwitch = false
        try {
            val checkStateConnection = withContext(IO) {
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

    private fun login(url: String, username: String, password: String) {
        val getLoginString: String =
            url + "login?username=" + username + "&password=" + password

        // Try to login
        val loginForm: Connection.Response = Jsoup.connect(getLoginString)
            .followRedirects(true)
            .method(Connection.Method.GET)
            .timeout(10000)
            .header("Connection", "close")
            .execute()

        val loginCookies = loginForm.cookie("ARDUINOSESSIONID")

        cookieEditor.putString("cookies", loginCookies)
        cookieEditor.apply()
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
        //editor.commit()
        editor.apply()
    }
}
