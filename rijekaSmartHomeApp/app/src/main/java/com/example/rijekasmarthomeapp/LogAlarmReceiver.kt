package com.example.rijekasmarthomeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jjoe64.graphview.series.DataPoint
import java.lang.reflect.Type
import java.util.*

private lateinit var deviceListPreferences: SharedPreferences
private lateinit var editor: SharedPreferences.Editor
/*
private lateinit var tempTempPref: SharedPreferences
private lateinit var timeTempPref: SharedPreferences
private lateinit var tempTempEditor: SharedPreferences.Editor
private lateinit var timeTempEditor: SharedPreferences.Editor
*/

class LogAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            deviceListPreferences = context.getSharedPreferences("devicesList", 0)
            /*
            tempTempPref = context.getSharedPreferences("tempTemp", 0)
            timeTempPref = context.getSharedPreferences("timeTemp", 0)
            */
        }

        editor = deviceListPreferences.edit()

        /*
        tempTempEditor = tempTempPref.edit()
        timeTempEditor = timeTempPref.edit()
        */

        var db = context?.let { dataBaseHelpter(it) }

        /* // Uncomment for deleting all data from log table
        if (db != null) {
            db.deleteAllData()
        }*/

        if (db != null) {
            var devicesList = getDevicesList()
            for (device in devicesList) {
                when (device) {
                    is WaterHeater -> {
                        val deviceName = "water_heater_" + device.id_num.toString()
                        val currTimeInMillis = Calendar.getInstance().timeInMillis
                        if (device.state == "ON") db.insert(deviceName, device.temperature, currTimeInMillis)
                        else db.insert(deviceName, "", currTimeInMillis)
                    }
                    is Heater -> {
                        val deviceName = "heater_" + device.id_num.toString()
                        val currTimeInMillis = Calendar.getInstance().timeInMillis
                        if (device.state == "ON") db.insert(deviceName, device.temperature, currTimeInMillis)
                        else db.insert(deviceName, "", currTimeInMillis)
                        /*
                        if (tempTempPref.getString("tempTemp", null) == null || timeTempPref.getString("timeTemp", null) == null) {
                            tempTempEditor.putString("tempTemp", device.temperature)
                            timeTempEditor.putString("timeTemp", currTimeInMillis.toString())

                            tempTempEditor.apply()
                            timeTempEditor.apply()
                        }*/
                    }
                    is AirConditioner -> {
                        val deviceName = "ac_" + device.id_num.toString()
                        val currTimeInMillis = Calendar.getInstance().timeInMillis
                        if (device.state == "ON") db.insert(deviceName, device.temperature, currTimeInMillis)
                        else db.insert(deviceName, "", currTimeInMillis)
                    }
                }
            }
            /*
            val rs: Cursor = db.getAllData()
            rs.moveToFirst()
            println(rs.getString(rs.getColumnIndex("temperature")))
            println(rs.getString(rs.getColumnIndex("device_name")))
            println(rs.getString(rs.getColumnIndex("time")))
            if (!rs.isClosed) rs.close()*/
        }

    }

    fun getDevicesList(): MutableList<Device> {
        var arrayItems: MutableList<Device> = mutableListOf()
        val serializedObject: String? =
            deviceListPreferences.getString("devicesList", null)
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