package com.phoenix.pillreminder.feature_alarms.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.AdapterMedicinesDataBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine

class MedicinesDataAdapter(
    private val context: Context,
    private val onMedicineClicked: (Medicine) -> Unit
): RecyclerView.Adapter<MedicinesDataAdapter.MedicinesDataViewHolder>() {
    private val medicineList = ArrayList<Medicine>()

    fun setAlarmsAndList(medicines: List<Medicine>, filter: String? = null){
        val newList = if(filter.isNullOrEmpty()){
            medicines
        } else {
            medicines.filter{ it.name.contains(filter, true) }
        }

        val diffCallback = object: DiffUtil.Callback(){
            override fun getOldListSize(): Int = medicineList.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return medicineList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return medicineList[oldItemPosition] == newList[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        medicineList.clear()
        medicineList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicinesDataViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val medicinesBinding = AdapterMedicinesDataBinding.inflate(layoutInflater, parent, false)
        return MedicinesDataViewHolder(medicinesBinding)
    }

    override fun getItemCount(): Int {
        return medicineList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: MedicinesDataViewHolder, position: Int) {
        val medicine = medicineList[position]
        holder.bind(medicine)
    }


    inner class MedicinesDataViewHolder(private val medicinesBinding: AdapterMedicinesDataBinding): RecyclerView.ViewHolder(medicinesBinding.root){
        fun bind(medicine: Medicine){

            medicinesBinding.apply{

                when(medicine.form){
                    "pill" -> {
                        ivMedicineType.setImageResource(R.drawable.ic_pill_coloured)
                        tvQuantityStrength.text = context.getString(R.string.quantity_pills, medicine.quantity.toInt())
                    }
                    "pomade" -> {
                        ivMedicineType.setImageResource(R.drawable.ic_ointment)
                        tvQuantityStrength.text = context.getString(R.string.quantity_application)
                    }
                    "injection" -> {
                        ivMedicineType.setImageResource(R.drawable.ic_injection)
                        when(medicine.unit){
                            "mL" -> tvQuantityStrength.text = context.getString(R.string.quantity_ml, medicine.quantity)
                            "syringe" -> tvQuantityStrength.text = context.getString(R.string.quantity_syringe, medicine.quantity.toInt())
                        }
                    }
                    "drop" -> {
                        ivMedicineType.setImageResource(R.drawable.ic_dropper)
                        tvQuantityStrength.text = context.getString(R.string.quantity_drops, medicine.quantity.toInt())
                    }
                    "inhaler" -> {
                        ivMedicineType.setImageResource(R.drawable.ic_inhalator)
                        when(medicine.unit){
                            "mg" -> tvQuantityStrength.text = context.getString(R.string.quantity_inhalator, medicine.quantity)
                            "puff" -> tvQuantityStrength.text = context.resources.getQuantityString(R.plurals.quantity_puffs, medicine.quantity.toInt(), medicine.quantity.toInt())
                            "mL" -> tvQuantityStrength.text = context.getString(R.string.quantity_ml, medicine.quantity)
                        }
                    }
                    "liquid" -> {
                        ivMedicineType.setImageResource(R.drawable.ic_liquid)
                        tvQuantityStrength.text = context.getString(R.string.quantity_ml, medicine.quantity)
                    }
                }

                tvMedicineName.text = medicine.name

                tvTreatmentState.text = when {
                    isTreatmentOngoing(medicine) -> context.getString(R.string.ongoing_treatment)
                    medicine.startDate > System.currentTimeMillis() -> context.getString(R.string.treatment_hasnt_started_yet)
                    else -> context.getString(R.string.treatment_ended)
                }

                tvTreatmentState.setTextColor(
                    if(isTreatmentOngoing(medicine)){
                        ContextCompat.getColor(context, R.color.green)
                    } else {
                        ContextCompat.getColor(context, R.color.button_gray)
                    }
                )

                root.setOnClickListener {
                   onMedicineClicked(medicine)
                }

                ibDetails.setOnClickListener {
                    onMedicineClicked(medicine)
                }

            }
        }

    }

    private fun isTreatmentOngoing(medicine: Medicine): Boolean{
        return (System.currentTimeMillis() in medicine.startDate..medicine.endDate) && medicine.isActive
    }
}

