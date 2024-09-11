package com.phoenix.pillreminder.feature_alarms.presentation.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.AdapterListMedicinesBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val HOUR_24_FORMAT = "HH:mm"
private const val HOUR_12_FORMAT = "hh:mm a"

class RvMedicinesListAdapter (private val showDeleteAlarmDialog: (Medicine) -> Unit,
                              private val showDeleteAllAlarmsDialog: (Medicine) -> Unit,
                              private val markMedicineUsage: (Medicine) -> Unit,
                              private val markMedicinesAsSkipped: (Medicine) -> Unit,
                              private val goToEditMedicines: (Medicine) -> Unit,
                              private val showEndTreatmentDialog: (Medicine) -> Unit) : RecyclerView.Adapter<MyViewHolder>() {

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
        holder.bind(medicineList[position], holder, showDeleteAlarmDialog, showDeleteAllAlarmsDialog,
            markMedicineUsage, markMedicinesAsSkipped, goToEditMedicines, showEndTreatmentDialog)
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


    fun bind(medicine: Medicine,
             holder: MyViewHolder,
             showDeleteAlarmDialog: (Medicine) -> Unit,
             showDeleteAllAlarmsDialog: (Medicine) -> Unit,
             markMedicineUsage: (Medicine) -> Unit,
             markMedicinesAsSkipped: (Medicine) -> Unit,
             goToEditMedicines: (Medicine) -> Unit,
             showEndTreatmentDialog: (Medicine) -> Unit){

        val context = holder.itemView.context
        val currentTimeInMillis = System.currentTimeMillis()

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
                "pill" -> {
                    val quantity = medicine.quantity.toInt()
                    tvQuantity.text = context.resources.getQuantityString(R.plurals.take_pill, quantity, quantity)
                }
                "injection" -> {
                    when(medicine.unit) {
                        "mL" -> {
                            tvQuantity.text = context.getString(R.string.take_injection_ml, medicine.quantity.toString())
                        }
                        "syringe" -> {
                            val quantity = medicine.quantity.toInt()
                            tvQuantity.text = context.resources.getQuantityString(R.plurals.take_injection_syringe, quantity, quantity)
                        }
                    }
                }
                "liquid" -> {
                    tvQuantity.text = context.getString(R.string.take_liquid, medicine.quantity.toString())
                }
                "drop" -> {
                    val quantity = medicine.quantity.toInt()
                    tvQuantity.text = context.resources.getQuantityString(R.plurals.take_drops, quantity, quantity)
                }
                "inhaler" -> {
                    when(medicine.unit){
                        "mg" -> {
                            tvQuantity.text = context.getString(R.string.inhale_mg, medicine.quantity.toString())
                        }
                        "puff" -> {
                            val quantity = medicine.quantity.toInt()
                            tvQuantity.text = context.resources.getQuantityString(R.plurals.inhale_puff, quantity, quantity)
                        }
                        "mL" -> {
                            tvQuantity.text = context.getString(R.string.inhale_mL, medicine.quantity.toString())
                        }
                    }
                }
                "pomade" -> {
                    tvQuantity.text = context.getString(R.string.apply_pomade)
                }
            }

            when(medicine.medicineWasTaken){
                true -> {
                    tvMedicineTaken.isVisible = true

                    if(medicine.form == "pomade" || medicine.form == "inhaler" || medicine.form == "injection" || medicine.form == "drops" || medicine.form == "liquid"){
                        tvMedicineTaken.text = context.getString(R.string.medicine_already_used)
                    } else{
                        tvMedicineTaken.text = context.getString(R.string.medicine_already_taken)
                    }

                }
                false -> {
                    tvMedicineTaken.isVisible = true

                    when (medicine.wasSkipped) {
                        true -> {
                            tvMedicineTaken.text =
                                context.getString(R.string.medicine_skipped)
                        }
                        false -> {
                            if(medicine.form == "pomade" || medicine.form == "inhaler" || medicine.form == "injection" || medicine.form == "drops" || medicine.form == "liquid"){
                                tvMedicineTaken.text = context.getString(R.string.medicine_not_used_yet)
                            } else{
                                tvMedicineTaken.text = context.getString(R.string.medicine_not_taken_yet)
                            }
                        }
                    }
                }

            }

           ivMenu.setOnClickListener {
               val popup = PopupMenu(context, ivMenu)
               popup.inflate(R.menu.options_menu)

               popup.setOnMenuItemClickListener { menuItem ->
                   when (menuItem.itemId) {
                       R.id.menu1 -> {
                           showDeleteAlarmDialog(medicine)
                           true
                       }
                       R.id.menu2 -> {
                           showDeleteAllAlarmsDialog(medicine)
                           true
                       }
                       R.id.menu3 -> {
                           markMedicinesAsSkipped(medicine)
                           true
                       }
                       R.id.menu4 -> {
                           goToEditMedicines(medicine)
                           true
                       }
                       R.id.menu5 -> {
                           showEndTreatmentDialog(medicine)
                           true
                       }
                       else -> false
                   }
               }
               popup.show()
           }

            btnMarkUsage.visibility =
                if (currentTimeInMillis > medicine.alarmInMillis && !medicine.medicineWasTaken && !medicine.wasSkipped){
                    View.VISIBLE
                }  else View.GONE

            btnMarkUsage.setOnClickListener {
                markMedicineUsage(medicine)
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