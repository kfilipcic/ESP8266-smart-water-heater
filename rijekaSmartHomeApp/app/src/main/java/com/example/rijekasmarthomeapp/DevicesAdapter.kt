package com.example.rijekasmarthomeapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.device_item.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.jsoup.Jsoup
import java.lang.ClassCastException
import java.lang.Exception

class DevicesAdapter(context: Context, private val devicesList: MutableList<Device>, private val cookies: Map<String, String>, private val url: String) :

    RecyclerView.Adapter<DevicesAdapter.MyViewHolder>() {

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

        deviceView.setOnClickListener {
            deviceView.context.startActivity(intent.putExtra("title", devicesList[holder.adapterPosition].name))
        }

        holder.deviceView.deviceImageBtn.setOnClickListener{
            try {
                GlobalScope.launch(IO) {
                    Jsoup.connect(url + "water_heater_1_switch.html")
                        .cookies(cookies)
                        .get()
                    println(url)
                    withContext(Dispatchers.Main) {
                        println("btj")
                        try {
                            adapterCallback.onMethodCallback(devicesList[holder.adapterPosition], url, cookies)
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
        val device: Device = devicesList[position]

        holder.deviceView.deviceNameTV.text = device.name

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

    override fun getItemCount() = devicesList.size
}

interface AdapterCallback {
    suspend fun onMethodCallback(device: Device, url: String, cookies: Map<String, String>)
}