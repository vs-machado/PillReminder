package com.phoenix.pillreminder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.phoenix.pillreminder.databinding.AdapterListMedicinesBinding
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDatabase
import com.phoenix.pillreminder.fragments.HomeFragment
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel
import com.phoenix.pillreminder.model.MedicinesViewModelFactory

class RvMedicinesListAdapter(private val sharedViewModel: AlarmSettingsSharedViewModel): RecyclerView.Adapter<MyViewHolder>() {

    private lateinit var viewModel: MedicinesViewModel
    private val medicineList = ArrayList<Medicine>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val medicinesBinding = AdapterListMedicinesBinding.inflate(layoutInflater, parent, false)

        val dao = MedicineDatabase.getInstance(parent.context).medicineDao()
        val factory = MedicinesViewModelFactory(dao)

        viewModel = ViewModelProvider(parent.context as HomeFragment, factory)[MedicinesViewModel::class.java]

        return MyViewHolder(medicinesBinding)
    }

    override fun getItemCount(): Int {
        return medicineList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(medicineList[position])
    }

    fun setList(medicines: List<Medicine>){
        medicineList.clear()
        medicineList.addAll(medicines)
    }


}

class MyViewHolder(private val medicinesBinding: AdapterListMedicinesBinding):RecyclerView.ViewHolder(medicinesBinding.root){
    fun bind(medicine: Medicine){
        medicinesBinding.apply{
            tvHour.text = "${medicine.alarmHour} +:+ ${medicine.alarmMinute}"
            tvMedicineName.text = medicine.name
            tvQuantity.text = "Take + ${medicine.quantity} + ${medicine.form}s"

           // Needs to set the imageView depending on medicine form
        }
    }
}