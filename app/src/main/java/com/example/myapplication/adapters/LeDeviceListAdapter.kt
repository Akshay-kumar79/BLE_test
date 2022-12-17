package com.example.myapplication.adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder


class LeDeviceListAdapter(private val context: Context, private val onDeviceClickListener: (device: BluetoothDevice) -> Unit) : RecyclerView.Adapter<LeDeviceListAdapter.MyViewHolder>() {

    val devices = mutableListOf<BluetoothDevice>()

    fun addDevice(device: BluetoothDevice) {
        if (!devices.contains(device)) {
            devices.add(device)
        }
        notifyDataSetChanged()
    }

    fun clear(){
        devices.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = devices[position].name
        holder.textView.setOnClickListener {
            onDeviceClickListener(devices[position])
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    class MyViewHolder(itemView: View) : ViewHolder(itemView) {

        val textView = itemView.findViewById<TextView>(android.R.id.text1)
    }
}