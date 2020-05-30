package com.example.rijekasmarthomeapp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.device_item.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.Serializable
import java.lang.ClassCastException
import java.lang.Exception
import java.lang.reflect.Type
import java.net.SocketTimeoutException

class DevicesAdapter(
    private var context: Context,
    private val url: String
) :
    RecyclerView.Adapter<DevicesAdapter.MyViewHolder>() {

    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var cookiePreferences: SharedPreferences
    private lateinit var cookieEditor: SharedPreferences.Editor
    private val adapterCallback: AdapterCallback
    private var cookie: String? = null

    init {
        try {
            adapterCallback = (context as AdapterCallback)
        } catch (e: Exception) {
            throw Exception("Activity must implement AdapterCallback.", e)
        }
    }

    class MyViewHolder(val deviceView: View) : RecyclerView.ViewHolder(deviceView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val deviceView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)

        cookiePreferences = parent.context.getSharedPreferences("cookies", 0)
        cookieEditor = cookiePreferences.edit()

       cookie = cookiePreferences.getString("cookies", null)


        val holder = MyViewHolder(deviceView)
        val intent = Intent(deviceView.context, DeviceDialog::class.java)
            //.putExtra("cookies", cookies as Serializable)

        deviceView.setOnClickListener {
            deviceView.context.startActivity(
                intent
                    .putExtra("position", holder.adapterPosition)
            )
        }

        holder.deviceView.deviceImageBtn.setOnClickListener {
            // Switch state

            try {
                GlobalScope.launch(IO) {
                    var devicesList: MutableList<Device> = getDevicesList()
                    var switchUrl: String = "default"
                    when (devicesList[holder.adapterPosition]) {
                        is WaterHeater -> switchUrl =
                            "water_heater_" + devicesList[holder.adapterPosition].id_num.toString() + "_switch.html"
                        is Heater -> switchUrl =
                            "heater_" + devicesList[holder.adapterPosition].id_num.toString() + "_switch.html"
                        is AirConditioner -> {
                            val acRemoteIntent =
                                Intent(deviceView.context, ACRemoteDialog::class.java)
                                    .putExtra(
                                        "title",
                                        devicesList[holder.adapterPosition].name
                                    )
                                    .putExtra("position", holder.adapterPosition)
                                    //.putExtra("cookies", cookies)
                           // val bundle: Bundle = Bundle()
                           // bundle.putSerializable("cookies", cookies)
                            (context as MainScreen).startActivityForResult(acRemoteIntent, 0)
                            /*deviceView.context.startActivity(
                                acRemoteIntent.putExtra(
                                    "title",
                                    devicesList[holder.adapterPosition].name
                                )
                                    .putExtra("position", holder.adapterPosition)
                                    .putExtra("cookies", cookies)
                            )*/
                            return@launch
                        }
                    }
                    try {
                        Jsoup.connect(url + switchUrl)
                            //.cookies(cookies)
                            .cookie("ARDUINOSESSIONID", cookie)
                            .get()
                        withContext(Dispatchers.Main) {
                            try {
                                adapterCallback.onMethodCallback(
                                    getDevicesList()[holder.adapterPosition],
                                    url
                                )
                                notifyDataSetChanged()
                                deviceView.invalidate()
                            } catch (e: ClassCastException) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        System.err.println("Session expired! Returning to the login screen...")
                        val loginIntent: Intent = Intent(context, MainActivity::class.java)
                            .putExtra("errorCode", -1)
                        context.startActivity(loginIntent)

                        e.printStackTrace()
                    }
                }
            } catch (e: SocketTimeoutException) {
                System.err.println("Session expired! Returning to the login screen...")
                val loginIntent: Intent = Intent(context, MainActivity::class.java)
                    .putExtra("errorCode", -1)
                context.startActivity(loginIntent)

                e.printStackTrace()
            } catch (e: Exception) {
                System.err.println("Connecting with server and/or calling method MainScreen.getDataFromServer() unsuccessful")
                e.printStackTrace()
            }
        }

        holder.deviceView.graphImageBtn.setOnClickListener {
            val graphIntent = Intent(context, GraphActivity::class.java)
                .putExtra("position", holder.adapterPosition)
            context.startActivity(graphIntent)
        }

        return holder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        var device: Device = getDevicesList()[position]
        var type = ""

        suspend fun getStateFromServer(
            deviceName: String,
            checkStateUrl: String
        ) {
            try {
                val checkStateConnection = withContext(Dispatchers.IO) {
                    Jsoup.connect(url + checkStateUrl)
                        //.cookies(cookies)
                        .cookie("ARDUINOSESSIONID", cookie)
                        .get()
                }
                val checkStatePage = JSONObject(checkStateConnection.body().text())

                when (checkStatePage.get(deviceName).toString()) {
                    "0" -> device.state = "ON" // Yes, zero is for ON
                    "1" -> device.state = "OFF"
                }

            } catch (e: Exception) {
                System.err.println("Error getting device state data from the server!")
                System.err.println(url + checkStateUrl)

                (context as Activity).finish()

                (context as Activity).overridePendingTransition(0, 0)

                val loginIntent = Intent(context, MainActivity::class.java)
                    .putExtra("errorCode", -1)
                context.startActivity(loginIntent)

                (context as Activity).overridePendingTransition(0, 0)

                e.printStackTrace()
                return
            }
        }

        suspend fun getTemperatureFromServer(
            deviceName: String,
            checkTempUrl: String
        ) {
            try {
                val checkTempConnection = withContext(Dispatchers.IO) {
                    Jsoup.connect(url + checkTempUrl)
                        //.cookies(cookies)
                        .cookie("ARDUINOSESSIONID", cookie)
                        .get()
                }
                val checkTempPage = JSONObject(checkTempConnection.body().text())

                when (checkTempPage.get(deviceName).toString()) {
                    "0" -> device.state = "ON" // Yes, zero is for ON
                    "1" -> device.state = "OFF"
                }

                when (device) {
                    is WaterHeater -> device.temperature = checkTempPage.get(deviceName).toString()
                    is Heater -> device.temperature = checkTempPage.get(deviceName).toString()
                    is AirConditioner -> device.temperature =
                        checkTempPage.get(deviceName).toString()
                }

                val devList: MutableList<Device> = getDevicesList()
                devList[position] = device
                setDevicesList("devicesList", devList)

            } catch (e: Exception) {
                System.err.println("Error getting device temperature data from the server!")
                System.err.println(url + checkTempUrl)
                e.printStackTrace()

                (context as Activity).finish()

                val loginIntent = Intent(context, MainActivity::class.java)
                    .putExtra("errorCode", -1)
                context.startActivity(loginIntent)
            }
        }


        holder.deviceView.deviceNameTV.text = device.name

        when (device) {
            is WaterHeater -> {
                type = "water_heater"
                val checkStateUrl: String =
                    type + "_" + device.id_num.toString() + "_" + "check.html"
                val checkTempUrl: String =
                    type + "_" + device.id_num.toString() + "_" + "temperature.html"
                GlobalScope.launch(IO) {
                    getStateFromServer(type, checkStateUrl)
                    getTemperatureFromServer(type, checkTempUrl)

                    withContext(Dispatchers.Main) {
                        holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_noinfo)
                        holder.deviceView.tempTV.text =
                            holder.deviceView.context.getString(
                                R.string.tempText,
                                device.temperature
                            )
                        when (device.state) {
                            "ON" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_on)
                            "OFF" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_off)
                        }

                        holder.deviceView.statusTV.text =
                            holder.deviceView.context.getString(R.string.statusText, device.state)
                        holder.deviceView.deviceNameTV.text = device.name

                        val devList: MutableList<Device> = getDevicesList()
                        devList[position] = device
                        setDevicesList("devicesList", devList)
                    }
                }
            }
            is Heater -> {
                type = "heater"
                val checkStateUrl: String =
                    type + "_" + device.id_num.toString() + "_" + "check.html"
                val checkTempUrl: String =
                    type + "_" + device.id_num.toString() + "_" + "temperature.html"

                GlobalScope.launch(IO) {
                    getStateFromServer(type, checkStateUrl)
                    getTemperatureFromServer(type, checkTempUrl)

                    withContext(Dispatchers.Main) {
                        holder.deviceView.deviceImageBtn.setImageResource(R.drawable.heater_noinfo)
                        holder.deviceView.tempTV.text =
                            holder.deviceView.context.getString(
                                R.string.tempText,
                                device.temperature
                            )
                        when (device.state) {
                            "ON" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.heater_on)
                            "OFF" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.heater_off)
                        }

                        holder.deviceView.statusTV.text =
                            holder.deviceView.context.getString(R.string.statusText, device.state)
                        holder.deviceView.deviceNameTV.text = device.name

                        val devList: MutableList<Device> = getDevicesList()
                        devList[position] = device
                        setDevicesList("devicesList", devList)
                    }
                }
            }
            is AirConditioner -> {
                type = "ac"
                val checkStateUrl: String =
                    type + "_" + device.id_num.toString() + "_" + "check.html"
                val checkTempUrl: String =
                    type + "_" + device.id_num.toString() + "_" + "temperature.html"

                GlobalScope.launch(IO) {
                    getStateFromServer(type, checkStateUrl)
                    getTemperatureFromServer(type, checkTempUrl)

                    withContext(Dispatchers.Main) {
                        holder.deviceView.deviceImageBtn.setImageResource(R.drawable.ac_noinfo)
                        holder.deviceView.tempTV.text =
                            holder.deviceView.context.getString(
                                R.string.tempText,
                                device.temperature
                            )
                        when (device.state) {
                            "ON" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.ac_on)
                            "OFF" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.ac_off)
                        }

                        holder.deviceView.statusTV.text =
                            holder.deviceView.context.getString(R.string.statusText, device.state)
                        holder.deviceView.deviceNameTV.text = device.name

                        val devList: MutableList<Device> = getDevicesList()
                        devList[position] = device
                        setDevicesList("devicesList", devList)
                    }
                }
            }
        }


    }

    override fun getItemCount(): Int {
        devicesListPreferences = context.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()
        return getDevicesList().size
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

interface AdapterCallback {
    suspend fun onMethodCallback(device: Device, url: String)
}

