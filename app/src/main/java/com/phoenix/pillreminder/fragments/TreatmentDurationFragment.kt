package com.phoenix.pillreminder.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AlarmScheduler
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel
import java.time.Instant
import java.time.ZoneId


class TreatmentDurationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: FragmentTreatmentDurationBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private lateinit var medicinesViewModel: MedicinesViewModel
    var alarmItem : AlarmItem? = null

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
            sharedViewModel.apply{

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
                            medicinesViewModel.insertMedicines(createMedicineAlarm())
                            /*Schedule alarm (if the user does not define a treatment period, it uses the same day to start to trigger alarms)
                            The -1 passed to the method indicates that there is no need to sum the alarm hours and minutes in millis
                            to the calendar instance*/
                            for(i in 0 until getAlarmHoursList().size){
                                setTimer(getUserDate(i), requireActivity(), i)
                            } //Extrair para um método

                            //notification()
                            clearTreatmentPeriod()

                            Toast.makeText(requireContext(),
                                "Alarms successfully created!",
                                Toast.LENGTH_LONG).show()
                            it.findNavController().navigate(R.id.action_treatmentDurationFragment_to_homeFragment)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showDateRangePickerAndCreateAlarm(){
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(requireActivity())

        sharedViewModel.apply{
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select the treatment duration:")
                .build()

            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                // Handle the selected date range
                val startDateMillis = selection.first
                val endDateMillis = selection.second

                //Extracts the treatment period and adds the user alarm hour to it in milliseconds
                extractDateComponents(startDateMillis, endDateMillis)
                medicinesViewModel.insertMedicines(createMedicineAlarm())

                /*for(i in 0 until getAlarmHoursList().size){
                    setTimer(getUserDate(i), requireActivity(), i)
                }*/
                for(i in 0 until getAlarmHoursList().size){
                    alarmItem = AlarmItem(
                        time = Instant.ofEpochMilli(getUserDate(i)).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        message = "test",
                        medicineName = "${getMedicineName()}",
                        medicineForm = "${getMedicineForm()}",
                        medicineQuantity = "${getMedicineQuantity()}",
                        alarmHour = "${getAlarmHour(i)}",
                        alarmMinute = "${getAlarmMinute(i)}"
                    )
                    alarmItem?.let(alarmScheduler::scheduleAlarm)
                }


                //notification()
                clearTreatmentPeriod()

                Toast.makeText(requireContext(),
                    "Alarms successfully created!",
                    Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_treatmentDurationFragment_to_homeFragment)
            }
            dateRangePicker.show(childFragmentManager, "DATE_RANGE_PICKER")
        }
    }
}
