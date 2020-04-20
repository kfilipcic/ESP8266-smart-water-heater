package com.example.rijekasmarthomeapp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DevicesAdapter(private val devicesList: MutableList<Device>) :
        RecyclerView.Adapter<DevicesAdapter.MyViewHolder>() {

        class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
                val textView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.my_text_view, parent, false) as TextView


                return MyViewHolder(textView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
                val device: Device = devicesList.get(position)
                System.out.println(device.name)
                holder.textView.text = device.name
        }

        override fun getItemCount() = devicesList.size
}