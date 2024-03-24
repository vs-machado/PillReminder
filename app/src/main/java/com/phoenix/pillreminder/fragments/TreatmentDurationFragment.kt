package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.MainActivity
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel


class TreatmentDurationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: FragmentTreatmentDurationBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private lateinit var medicinesViewModel: MedicinesViewModel

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

                lvTreatmentDuration.setOnItemClickListener { _, _, position, _ ->
                    when (position){
                        // User want to set a treatment period
                        0 -> {
                            showDateRangePickerAndCreateAlarm()
                        }
                        // User don't want to set a treatment period
                        1 -> {
                            when (getMedicineFrequency()){
                                "Every day" -> {
                                    medicinesViewModel.insertMedicines(allAlarmsOfTreatment(1L))
                                    // Needs to create alarms for a certain period. FI: 3 days and renew it with a WorkManager
                                }
                                "Every other day" -> {
                                    medicinesViewModel.insertMedicines(allAlarmsOfTreatment(2L))
                                    // Needs to create alarms for a certain period.
                                }
                                "Specific days of the week" -> {
                                    // Needs further implementation
                                }
                                "Every X days" -> {
                                    // Needs further implementation
                                }
                                "Every X weeks" -> {
                                    // Needs further implementation
                                }
                                "Every X months" -> {
                                    // Needs further implementation
                                }
                            }
                            /*Schedule alarm (if the user does not define a treatment period, it uses the same day to start to trigger alarms)
                            The -1 passed to the method indicates that there is no need to sum the alarm hours and minutes in millis
                            to the calendar instance*/
                            for(i in 0 until getAlarmHoursList().size){
                                setTimer(getUserDate(i), requireActivity(), i)
                            }

                            clearTreatmentPeriod()

                            Toast.makeText(requireContext(),
                                "Alarms successfully created!",
                                Toast.LENGTH_LONG).show()

                            popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun showDateRangePickerAndCreateAlarm(){
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


                when (getMedicineFrequency()){
                    "Every day" -> {
                        medicinesViewModel.insertMedicines(allAlarmsOfTreatment(1L))
                        createAlarmItemAndSchedule(requireActivity().applicationContext, 1L)
                    }
                    "Every other day" -> {
                        medicinesViewModel.insertMedicines(allAlarmsOfTreatment(2L))
                        createAlarmItemAndSchedule(requireActivity().applicationContext, 2L)
                    }
                    "Specific days of the week" -> {
                        // Needs further implementation
                    }
                    "Every X days" -> {
                        // Needs further implementation
                    }
                    "Every X weeks" -> {
                        // Needs further implementation
                    }
                    "Every X months" -> {
                        // Needs further implementation
                    }
                }

                //notification()
                clearTreatmentPeriod()

                Toast.makeText(requireContext(),
                    "Alarms successfully created!",
                    Toast.LENGTH_LONG).show()

                popBackStack()
            }
            dateRangePicker.show(childFragmentManager, "DATE_RANGE_PICKER")
        }
    }

    private fun popBackStack(){
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_down)
            .setExitAnim(R.anim.slide_out_down)
            .setPopEnterAnim(R.anim.slide_in_up)
            .setPopExitAnim(R.anim.slide_out_up)
            .build()

        findNavController().popBackStack(R.id.homeFragment, false)
        findNavController().navigate(R.id.homeFragment, null, navOptions)
    }
}
