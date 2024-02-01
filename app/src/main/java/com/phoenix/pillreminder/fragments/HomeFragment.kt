package com.phoenix.pillreminder.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.MainActivity
import com.phoenix.pillreminder.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.databinding.FragmentHomeBinding
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter
    private lateinit var medicinesViewModel: MedicinesViewModel
    private var toast: Toast? = null
    private val sharedViewModel: AlarmSettingsSharedViewModel by viewModels()

    private lateinit var selectedMedicine: Medicine


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)


        medicinesViewModel = ViewModelProvider(requireActivity(), (requireActivity() as MainActivity).factory)[MedicinesViewModel::class.java]


        binding.apply {
            toolbarHome.setupWithNavController(navController, appBarConfiguration)

            initRecyclerView()

            fabAddMedicine.setOnClickListener {
                it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
            }
        }
    }

    private fun initRecyclerView(){
        binding.rvMedicinesList.layoutManager = LinearLayoutManager(activity)
        adapter = RvMedicinesListAdapter{
            selectedMedicine: Medicine -> listItemClicked(selectedMedicine)
        }
        binding.rvMedicinesList.adapter = adapter

        displayMedicinesList()
    }

    private fun displayMedicinesList(){
        medicinesViewModel.medicines.observe(viewLifecycleOwner) {
            adapter.setList(it)
        }
    }

    private fun listItemClicked(medicine: Medicine){
        showDeleteAlarmDialog(medicine)
    }

    private fun showDeleteAlarmDialog(medicine: Medicine){
        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_delete_alarm_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMedicine: TextView = dialog.findViewById(R.id.tvMedicineAndHour)
        val btnDelete: Button = dialog.findViewById(R.id.btnDelete)
        val btnCancel: Button = dialog.findViewById(R.id.btnCancel)

        tvMedicine.text = "${medicine.name}\n${showTvAlarm(medicine.alarmHour, medicine.alarmMinute)}"

        btnDelete.setOnClickListener {
            medicinesViewModel.deleteMedicines(medicine)
            dialog.dismiss()
            showToast("Alarm successfully deleted!")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showToast( message: String){
        //Checks if a Toast is currently being displayed
        if (toast != null){
            toast?.cancel()
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        toast?.show()
    }

    private fun showTvAlarm(alarmHour: Int, alarmMinute: Int): String{
        val context = requireContext()
        return when {
            alarmHour < 10 && alarmMinute < 10 -> {
                // Format 3:8 (for instance) to 03:08
                val hour = context.getString(R.string.hour_minute_format, alarmHour.toString())
                val minute = context.getString(R.string.hour_minute_format, alarmMinute.toString())
                context.getString(R.string.tv_hour, hour, minute)
            }
            alarmHour < 10 && alarmMinute >= 10 -> {
                // Format 3:18 (for instance) to 03:18
                val hour = context.getString(R.string.hour_minute_format, alarmHour.toString())
                context.getString(R.string.tv_hour, hour, alarmMinute.toString())
            }
            alarmHour >= 10 && alarmMinute < 10 -> {
                // Format 13:8 (for instance) to 13:08
                val minute = context.getString(R.string.hour_minute_format, alarmMinute.toString())
                context.getString(R.string.tv_hour, alarmHour.toString(), minute)
            }
            else -> {
                // Format 13:18 (for instance) to 13:18
                context.getString(R.string.tv_hour, alarmHour.toString(), alarmMinute.toString())
            }
        }
    }
}