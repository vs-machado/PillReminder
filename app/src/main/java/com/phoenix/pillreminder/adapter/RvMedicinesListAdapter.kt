package com.phoenix.pillreminder.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.AdapterListMedicinesBinding
import com.phoenix.pillreminder.db.Medicine
import kotlinx.coroutines.selects.select
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val HOUR_24_FORMAT = "HH:mm"
private const val HOUR_12_FORMAT = "hh:mm a"

class RvMedicinesListAdapter (private val clickListener: (Medicine) -> Unit) : RecyclerView.Adapter<MyViewHolder>() {

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
        holder.bind(medicineList[position], holder, clickListener)
    }

    fun setList(medicines: List<Medicine>, selectedDate: Date){
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        val filteredList = medicines.filter { medicine ->
            val medicineCalendar = Calendar.getInstance().apply {
                timeInMillis = medicine.alarmInMillis
            }

            medicineCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    medicineCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                    medicineCalendar.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)
        }

        medicineList.clear()
        medicineList.addAll(filteredList)
        sortList()
        notifyDataSetChanged()
    }

    private fun sortList(){
        medicineList.sortWith(compareBy({it.alarmHour}, {it.alarmMinute}))
    }

}

class MyViewHolder(private val medicinesBinding: AdapterListMedicinesBinding):RecyclerView.ViewHolder(medicinesBinding.root){


    fun bind(medicine: Medicine, holder: MyViewHolder, clickListener: (Medicine) -> Unit){
        val context = holder.itemView.context

        //Formats the textview to show the hour in format HH:MM
        medicinesBinding.apply{
            when {
                DateFormat.is24HourFormat(context) -> {
                    formatHour(medicine.alarmHour, medicine.alarmMinute, HOUR_24_FORMAT).let{
                        tvHour.text = it
                    }
                }

                !DateFormat.is24HourFormat(context) -> {
                    formatHour(medicine.alarmHour, medicine.alarmMinute, HOUR_12_FORMAT).let{
                        tvHour.text = it
                    }
                }
            }

            when(medicine.form){
                "pill" -> ivMedicineType.setImageResource(R.drawable.ic_pill_coloured)
                "pomade" -> ivMedicineType.setImageResource(R.drawable.ic_ointment)
                "injection" -> ivMedicineType.setImageResource(R.drawable.ic_injection)
                "drop" -> ivMedicineType.setImageResource(R.drawable.ic_dropper)
                "inhaler" -> ivMedicineType.setImageResource(R.drawable.ic_inhalator)
                "liquid" -> ivMedicineType.setImageResource(R.drawable.ic_liquid)
            }

            tvMedicineName.text = medicine.name


            when(medicine.form){
                "pill" -> tvQuantity.text = context.getString(R.string.take_pill, medicine.quantity.toInt().toString(), medicine.form)
                "injection" -> tvQuantity.text = context.getString(R.string.take_injection, medicine.quantity.toString())
                "liquid" -> tvQuantity.text = context.getString(R.string.take_liquid, medicine.quantity.toString())
                "drop" -> tvQuantity.text = context.getString(R.string.take_drops, medicine.quantity.toInt().toString(), medicine.form)
                "inhaler" -> tvQuantity.text = context.getString(R.string.inhale, medicine.quantity.toString())
                "pomade" -> tvQuantity.text = context.getString(R.string.apply_pomade, medicine.quantity.toString())
            }

            when(medicine.medicineWasTaken /*and compare user hour to alarm hour in millis*/){
                true -> {
                    tvMedicineTaken.isVisible = true
                    tvMedicineTaken.text = "Medicine already taken" }
                false -> {
                    tvMedicineTaken.isVisible = true
                    tvMedicineTaken.text = "Medicine not taken yet."}
            }

           ivDelete.setOnClickListener {
                clickListener(medicine)
           }
        }
    }

     private fun formatHour(hour: Int, minute: Int, pattern: String): String{
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(calendar.time)
    }
}