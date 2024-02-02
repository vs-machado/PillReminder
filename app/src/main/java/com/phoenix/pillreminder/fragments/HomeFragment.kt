package com.phoenix.pillreminder.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.phoenix.pillreminder.model.MedicinesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter
    private lateinit var medicinesViewModel: MedicinesViewModel
    private var toast: Toast? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            setCreditsVisibility()
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

        tvMedicine.text = context?.getString(R.string.tv_alarm_and_hour, medicine.name, showTvAlarm(medicine.alarmHour, medicine.alarmMinute))

        btnDelete.setOnClickListener {
            medicinesViewModel.deleteMedicines(medicine)
            dialog.dismiss()
            showToast()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showToast(){
        //Checks if a Toast is currently being displayed
        if (toast != null){
            toast?.cancel()
        }
        toast = Toast.makeText(context, "Alarm successfully deleted!", Toast.LENGTH_LONG)
        toast?.show()
    }

    private fun showTvAlarm(alarmHour: Int, alarmMinute: Int): String{
        val context = requireContext()
        when {
            DateFormat.is24HourFormat(context) -> {
                return formatHour(alarmHour, alarmMinute, "HH:mm")
            }
            !DateFormat.is24HourFormat(context) -> {
                return formatHour(alarmHour, alarmMinute, "hh:mm a")
            }
        }
        return ""
    }

    private fun formatHour(hour: Int, minute: Int, pattern: String): String{
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun setCreditsVisibility(){
        if(binding.rvMedicinesList.adapter?.itemCount!! > 0){
            binding.tvCredits.isVisible = true
            return
        }
        binding.tvCredits.isVisible = false
    }
}