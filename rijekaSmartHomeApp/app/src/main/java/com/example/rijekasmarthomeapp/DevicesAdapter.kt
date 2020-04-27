package com.example.rijekasmarthomeapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.device_item.view.*

class DevicesAdapter(private val devicesList: MutableList<Device>) :
    RecyclerView.Adapter<DevicesAdapter.MyViewHolder>() {

    class MyViewHolder(val deviceView: View) : RecyclerView.ViewHolder(deviceView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val deviceView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)

        val holder = MyViewHolder(deviceView)

        val intent = Intent(deviceView.context, DeviceDialog::class.java)
        holder.deviceView.deviceImageBtn.setOnClickListener{
            deviceView.context.startActivity(intent.putExtra("title", devicesList[holder.adapterPosition].name))
        }

        return holder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val device: Device = devicesList[position]
        val status: String


        holder.deviceView.deviceNameTV.text = device.name

        if (device is WaterHeater) {
            holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_noinfo)

            holder.deviceView.tempTV.text = holder.deviceView.context.getString(R.string.tempText, device.waterTemperature)

            println("DevicesAdapter - device.waterTemperature: " + device.waterTemperature)

            if (device.state) {
                status = "ON"
                holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_on)
            }
            else {
                status = "OFF"
                holder.deviceView.deviceImageBtn.setImageResource(R.drawable.boiler_off)
            }

            holder.deviceView.statusTV.text = holder.deviceView.context.getString(R.string.statusText, status)
        }

        holder.deviceView.deviceNameTV.text = device.name
    }

    override fun getItemCount() = devicesList.size
}