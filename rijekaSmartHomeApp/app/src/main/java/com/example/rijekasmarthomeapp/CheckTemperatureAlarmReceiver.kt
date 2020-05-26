package com.example.rijekasmarthomeapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.jsoup.Jsoup
import java.lang.reflect.Type

@Suppress(
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "UNCHECKED_CAST", "unused"
)
class CheckTemperatureAlarmReceiver : BroadcastReceiver() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val baseUrl = "http://psih.duckdns.org/"

    @SuppressLint("CommitPrefEdits")
    override fun onReceive(context: Context, intent: Intent) {
        val cookies: Map<String, String> = intent.getSerializableExtra("cookies") as Map<String, String>

        devicesListPreferences = context.applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()

        val devicesList: MutableList<Device> = getDevicesList()

        try {
            GlobalScope.launch(IO) {
                var type = ""
                var minTemp = 0.0
                var maxTemp = 0.0
                var autoRegTemp = false

                for (device: Device in devicesList) {
                    when (device) {
                        is WaterHeater -> {
                            type = "water_heater"
                            minTemp = device.minTemp
                            maxTemp = device.maxTemp
                            autoRegTemp = device.autoRegulateTemperature
                        }
                        is Heater -> {
                            type = "heater"
                            minTemp = device.minTemp
                            maxTemp = device.maxTemp
                            autoRegTemp = device.autoRegulateTemperature
                        }
                        is AirConditioner -> {
                            type = "ac"
                            minTemp = device.minTemp
                            maxTemp = device.maxTemp
                            autoRegTemp = device.autoRegulateTemperature
                        }
                    }
                    val tempUrl = baseUrl + type + "_" + device.id_num.toString() + "_" + "temperature.html"
                    val temperatureHTML =
                        Jsoup.connect(tempUrl)
                            .cookies(cookies)
                            .get()
                    val temperaturePage = JSONObject(temperatureHTML.body().text())
                    val currentTemp: Double =
                        temperaturePage.get(type).toString().toDouble()

                    if (currentTemp < minTemp || currentTemp > maxTemp) {
                        // Don't show notifications if the device device is already working on going back to it's range values
                        if (autoRegTemp && (currentTemp < minTemp && device.state == "ON") || (currentTemp > maxTemp && device.state == "OFF")) return@launch

                        println("minTemp: $minTemp")
                        println("maxTemp: $maxTemp")
                        println("currentTemp$currentTemp")

                        //val cookies: MutableMap<String, String> = mutableMapOf<String, String>()

                        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
                        val notificationUtils = NotificationUtils(context)
                        // val notificationDetails = intent.getStringArrayListExtra("notif")

                        val notificationTitle: String = if (autoRegTemp) {
                            if (currentTemp < minTemp) {
                                "Turning on " + device.name + "..."
                            } else {
                                "Turning off " + device.name + "..."
                            }
                        } else {
                            "Temperature not in range!"
                        }
                        val notificationDescription: String = device.name + " is currently at " + currentTemp + "°C"

                        val notification = notificationUtils.getNotificationBuilder(
                            notificationTitle,
                            notificationDescription
                        ).build()
                        notificationUtils.getManager().notify(device.id_num, notification)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
