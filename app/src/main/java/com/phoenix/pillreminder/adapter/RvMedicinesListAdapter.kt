package com.phoenix.pillreminder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.AdapterListMedicinesBinding
import com.phoenix.pillreminder.db.Medicine


class RvMedicinesListAdapter : RecyclerView.Adapter<MyViewHolder>() {

    private val medicineList = ArrayList<Medicine>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val medicinesBinding = AdapterListMedicinesBinding.inflate(layoutInflater, parent, false)

        return MyViewHolder(medicinesBinding)
    }

    override fun getItemCount(): Int {
        return medicineList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(medicineList[position], holder)
    }

    fun setList(medicines: List<Medicine>){
        medicineList.clear()
        medicineList.addAll(medicines)
        notifyDataSetChanged()
    }


}

class MyViewHolder(private val medicinesBinding: AdapterListMedicinesBinding):RecyclerView.ViewHolder(medicinesBinding.root){
    fun bind(medicine: Medicine, holder: MyViewHolder){
        val context = holder.itemView.context

        //Formats the textview to show the hour in format HH:MM
        medicinesBinding.apply{
            if(medicine.alarmHour < 10){
                val hour = context.getString(R.string.hour_minute_format, medicine.alarmHour.toString())
                if(medicine.alarmMinute < 10){
                    val minute = context.getString(R.string.hour_minute_format, medicine.alarmMinute.toString())
                    //Format 3:8 (for instance) as 03:08
                    tvHour.text = context.getString(R.string.tv_hour, hour, minute)
                    return
                }
                //Format 3:08 (for instance) as 03:08
                tvHour.text = context.getString(R.string.tv_hour, hour, medicine.alarmMinute.toString())
            }
            else{
                tvHour.text = context.getString(R.string.tv_hour, medicine.alarmHour.toString(), medicine.alarmMinute.toString())
            }

            tvMedicineName.text = medicine.name


            when(medicine.form){
                "pill" -> tvQuantity.text = context.getString(R.string.take_pill, medicine.quantity.toString(), medicine.form)
                "injection" -> tvQuantity.text = context.getString(R.string.take_injection, medicine.quantity.toString(), medicine.form)
                "liquid" -> tvQuantity.text = context.getString(R.string.take_liquid, medicine.quantity.toString())
                "drop" -> tvQuantity.text = context.getString(R.string.take_drops, medicine.quantity.toString(), medicine.form)
                "inhaler" -> tvQuantity.text = context.getString(R.string.inhale, medicine.quantity.toString())
                "powder" -> tvQuantity.text = context.getString(R.string.apply_powder, medicine.quantity.toString())
            }

           // Needs to set the imageView depending on medicine form
        }
    }
}