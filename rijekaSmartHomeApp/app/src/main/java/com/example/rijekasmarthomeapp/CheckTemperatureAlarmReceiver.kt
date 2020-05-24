package com.example.rijekasmarthomeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.reflect.Type

@Suppress(
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class CheckTemperatureAlarmReceiver : BroadcastReceiver() {
    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val baseUrl = "http://psih.duckdns.org/"

    override fun onReceive(context: Context, intent: Intent) {
        val cookies = intent.getSerializableExtra("cookies") as Map<String, String>

        devicesListPreferences = context.applicationContext.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()
        var devicesList: MutableList<Device> = getDevicesList()

        try {
            GlobalScope.launch(IO) {
                var waterHeaterCount = 0
                var heaterCount = 0
                var acCount = 0
                var type = ""

                for (device: Device in devicesList) {
                    when (device) {
                        is WaterHeater -> {
                            type = "water_heater"
                            waterHeaterCount++
                        }
                        is Heater -> {
                            type = "heater"
                            heaterCount++
                        }
                        is AirConditioner -> {
                            type = "ac"
                            acCount++
                        }
                    }
                    val tempUrl = baseUrl + type + "_" + waterHeaterCount.toString() + "_" + "temperature.html"
                    println("url of tmep nez " +tempUrl )
                    val temperatureHTML =
                        Jsoup.connect(tempUrl)
                            .cookies(cookies)
                            .get()
                    val temperaturePage = JSONObject(temperatureHTML.body().text())
                    val currentTemp: Double =
                        temperaturePage.get(type).toString().toDouble()

                    if (currentTemp > 10) {

                        val cookies: MutableMap<String, String> = mutableMapOf<String, String>()

                        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
                        val notificationUtils = NotificationUtils(context)
                        //val notificationDetails:ArrayList<String> = intent.getStringArrayListExtra("notif")
                        val notificationDetails = intent.getStringArrayListExtra("notif")
                        val notification = notificationUtils.getNotificationBuilder(
                            notificationDetails[0],
                            notificationDetails[1]
                        ).build()
                        notificationUtils.getManager().notify(150, notification)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
