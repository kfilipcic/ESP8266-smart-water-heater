package com.example.rijekasmarthomeapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.device_item.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.jsoup.Jsoup
import java.io.Serializable
import java.lang.ClassCastException
import java.lang.Exception
import java.lang.reflect.Type

class DevicesAdapter(private var context: Context, private val cookies: Map<String, String>, private val url: String) :

    RecyclerView.Adapter<DevicesAdapter.MyViewHolder>() {

    private lateinit var devicesListPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private val adapterCallback:AdapterCallback
    init{
        try
        {
            adapterCallback = (context as AdapterCallback)
        }
        catch (e:Exception) {
            throw Exception("Activity must implement AdapterCallback.", e)
        }
    }

    class MyViewHolder(val deviceView: View) : RecyclerView.ViewHolder(deviceView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val deviceView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)


        val holder = MyViewHolder(deviceView)
        val intent = Intent(deviceView.context, DeviceDialog::class.java)
            .putExtra("cookies", cookies as Serializable)
        val acRemoteIntent = Intent(deviceView.context, ACRemoteDialog::class.java)

        deviceView.setOnClickListener {
            if (getDevicesList()[holder.adapterPosition] is AirConditioner)
                deviceView.context.startActivity(acRemoteIntent.putExtra("title", getDevicesList()[holder.adapterPosition].name)
                    .putExtra("position", holder.adapterPosition))
            else
                deviceView.context.startActivity(intent
                    .putExtra("position", holder.adapterPosition))
        }

        holder.deviceView.deviceImageBtn.setOnClickListener{
            try {
                GlobalScope.launch(IO) {
                    Jsoup.connect(url + "water_heater_1_switch.html")
                        .cookies(cookies)
                        .get()
                    withContext(Dispatchers.Main) {
                        try {
                            adapterCallback.onMethodCallback(getDevicesList()[holder.adapterPosition], url, cookies)
                            notifyDataSetChanged()
                            deviceView.invalidate()
                        } catch (e: ClassCastException) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                println("Connecting with server and/or calling method MainScreen.getDataFromServer() unsuccessful")
                e.printStackTrace()
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val device: Device = getDevicesList()[position]

        holder.deviceView.deviceNameTV.text = device.name

        println(device.state)
        if(device is WaterHeater) println("Omg" + device.waterTemperature)

        when (device) {
            is WaterHeater -> {
                holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_noinfo)
                holder.deviceView.tempTV.text = holder.deviceView.context.getString(R.string.tempText, device.waterTemperature)

                when(device.state) {
                    "ON" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_on)
                    "OFF" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_off)
                }
            }
            is Heater -> {
                holder.deviceView.deviceImageBtn.setImageResource(R.drawable.heater_noinfo)
                holder.deviceView.tempTV.text = holder.deviceView.context.getString(R.string.tempText, device.roomTemperature)

                when(device.state) {
                    "ON" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.heater_on)
                    "OFF" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.heater_off)
                }
            }
            is AirConditioner -> {
                holder.deviceView.deviceImageBtn.setImageResource(R.drawable.ac_noinfo)
                holder.deviceView.tempTV.text = holder.deviceView.context.getString(R.string.tempText, device.roomTemperature)

                when(device.state) {
                    "ON" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.ac_on)
                    "OFF" -> holder.deviceView.deviceImageBtn.setImageResource(R.drawable.ac_off)
                }
            }
        }

        holder.deviceView.statusTV.text = holder.deviceView.context.getString(R.string.statusText, device.state)
        holder.deviceView.deviceNameTV.text = device.name
    }

    override fun getItemCount(): Int  {
        devicesListPreferences = context.getSharedPreferences("devicesList", 0)
        editor = devicesListPreferences.edit()
        return getDevicesList().size
    }

    fun getDevicesList(): MutableList<Device> {
        var arrayItems: MutableList<Device> = mutableListOf()
        val serializedObject: String? =
            devicesListPreferences.getString("devicesList", null)
        if (serializedObject != null) {
            var gson : Gson = GsonBuilder().registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory
                    .of(Device::class.java)
                    .registerSubtype(WaterHeater::class.java )
                    .registerSubtype(Heater::class.java)
                    .registerSubtype(AirConditioner::class.java)
                        as RuntimeTypeAdapterFactory<Device>).create()
            val type: Type = TypeToken.getParameterized(MutableList::class.java, Device::class.java).type
            arrayItems = gson.fromJson(serializedObject, type)
        }
        return arrayItems
    }


    fun setDevicesList(key: String, list: MutableList<Device>) {
        var gson : Gson = GsonBuilder().registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory
                .of(Device::class.java)
                .registerSubtype(WaterHeater::class.java )
                .registerSubtype(Heater::class.java)
                .registerSubtype(AirConditioner::class.java)
                    as RuntimeTypeAdapterFactory<Device>).create()
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
    suspend fun onMethodCallback(device: Device, url: String, cookies: Map<String, String>)
}

