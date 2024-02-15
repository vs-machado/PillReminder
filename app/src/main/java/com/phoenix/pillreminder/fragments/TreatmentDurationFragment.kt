package com.phoenix.pillreminder.fragments

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.MainActivity
import com.phoenix.pillreminder.alarmscheduler.AlarmReceiver
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel



class TreatmentDurationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: FragmentTreatmentDurationBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private lateinit var medicinesViewModel: MedicinesViewModel
    private lateinit var pendingIntent: PendingIntent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            sharedViewModel.position--
            sharedViewModel.decreaseCurrentAlarmNumber()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTreatmentDurationBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        medicinesViewModel = ViewModelProvider(requireActivity(), (requireActivity() as MainActivity).factory)[MedicinesViewModel::class.java]

        binding.apply{
            toolbar.setupWithNavController(navController, appBarConfiguration)

            // Med forms list. User must select the desired type of med
            val list: MutableList<String> = mutableListOf("Yes, I do", "No, I don't")
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvTreatmentDuration.adapter = arrayAdapter

            lvTreatmentDuration.setOnItemClickListener { _, it, position, _ ->
                when (position){
                    0 -> {
                        showDateRangePickerAndCreateAlarm()
                    }
                    1 -> {
                        //Insert into database
                        medicinesViewModel.insertMedicines(sharedViewModel.createMedicineAlarm())
                        //Schedule alarm (if the user does not define a treatment period, it uses the same day to start to trigger alarms)
                        setTimer(sharedViewModel.getUserDate())
                        notification()
                        Toast.makeText(requireContext(),
                            "Alarms successfully created!",
                            Toast.LENGTH_LONG).show()
                        it.findNavController().navigate(R.id.action_treatmentDurationFragment_to_homeFragment)
                    }
                }

            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showDateRangePickerAndCreateAlarm(){
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select the treatment duration:")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            // Handle the selected date range
            val startDateMillis = selection.first
            val endDateMillis = selection.second

            //Extracts the treatment period and adds the alarm hour to it in milliseconds
            sharedViewModel.extractDateComponents(startDateMillis, endDateMillis)
            medicinesViewModel.insertMedicines(sharedViewModel.createMedicineAlarm())

            setTimer(sharedViewModel.getTreatmentStartDate())
            notification()

            Toast.makeText(requireContext(),
                "Alarms successfully created!",
                Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_treatmentDurationFragment_to_homeFragment)
        }
        dateRangePicker.show(childFragmentManager, "DATE_RANGE_PICKER")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setTimer(startDate: Long){
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(requireActivity(), AlarmReceiver::class.java)

        pendingIntent = PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE)

        if(!alarmManager.canScheduleExactAlarms()){
            //Needs to explain to user why he can't use the app
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startDate, pendingIntent)
    }

    private fun notification(){
        val name = "Time to take your medicine!"
        val description = "Do not forget to mark the medicine as taken."
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel("Notify", name, importance)
        channel.description = description

        val notificationManager = requireActivity().getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

    }
}
