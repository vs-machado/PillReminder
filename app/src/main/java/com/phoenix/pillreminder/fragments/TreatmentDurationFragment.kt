package com.phoenix.pillreminder.fragments

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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.MainActivity
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AlarmItemManager
import com.phoenix.pillreminder.alarmscheduler.AlarmScheduler
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel


class TreatmentDurationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: FragmentTreatmentDurationBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private lateinit var medicinesViewModel: MedicinesViewModel
    private var alarmItem : AlarmItem? = null

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

                lvTreatmentDuration.setOnItemClickListener { _, _, position, _ ->
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
                            }

                            //notification()
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

                for(i in 0 until getAlarmHoursList().size){
                    alarmItem = AlarmItem(
                        time = millisToDateTime(getAlarmInMillis(i)),
                        medicineName = "${getMedicineName()}",
                        medicineForm = "${getMedicineForm()}",
                        medicineQuantity = "${getMedicineQuantity()}",
                        alarmHour = "${getAlarmHour(i)}",
                        alarmMinute = "${getAlarmMinute(i)}"
                    )
                    //Add the alarm item to alarmItemList
                    sharedViewModel.addAlarmItem(alarmItem!!)
                    Log.i("ALARMLIST", "${sharedViewModel.getAlarmItemList()}")
                    alarmItem?.let(alarmScheduler::scheduleAlarm)
                }

                //Serializes the list in a JSON file
                AlarmItemManager.saveAlarmItems(requireContext().applicationContext, getAlarmItemList())
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
