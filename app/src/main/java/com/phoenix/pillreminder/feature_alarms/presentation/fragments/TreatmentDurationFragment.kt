package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.pillreminder.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TreatmentDurationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: FragmentTreatmentDurationBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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

        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.colorPrimary,
            R.color.white_ice,
            R.color.grayed_blue,
            R.color.dark_gray,
            isAppearanceLightStatusBar = false,
            isAppearanceLightNavigationBar = true,
            isAppearanceLightStatusBarNightMode = false,
            isAppearanceLightNavigationBarNightMode = false
        )

        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE
        requireActivity().findViewById<View>(R.id.divider).visibility = View.GONE
        requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine).visibility = View.GONE

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.apply{
            sharedViewModel.apply{

                toolbar.setupWithNavController(navController, appBarConfiguration)

                // Med forms list. User must select the desired type of med
                val list: MutableList<String> = mutableListOf(getString(R.string.yes_i_do),
                    getString(R.string.no_i_don_t))
                val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
                lvTreatmentDuration.adapter = arrayAdapter

                lvTreatmentDuration.setOnItemClickListener { _, _, position, _ ->
                    when (position){
                        // User want to set a treatment period
                        0 -> {
                            showDateRangePickerAndCreateAlarm()
                        }
                        /* User don't want to set a treatment period. The app will set a temporary treatment period (search for
                        setTemporaryTreatmentDate method on sharedViewModel). If user doesn't remove the alarm, it will be renewed.*/
                        1 -> {
                            setTemporaryPeriod()

                            val startDateMillis = System.currentTimeMillis()
                            val endDateMillis = setTemporaryTreatmentEndDate(startDateMillis)

                            extractDateComponents(startDateMillis, endDateMillis, false)

                            // Catches the workerID to cancel it if needed. workerID will be stored in the database.
                            val workerID = createRescheduleWorker(requireContext().applicationContext)
                            val interval = getInterval().toLong()

                            when (getMedicineFrequency()){
                                MedicineFrequency.EveryDay -> {
                                    medicinesViewModel.insertMedicines(getAlarmsList(1L, workerID, null, true))
                                    createAlarmItemAndSchedule(requireActivity().applicationContext, 1L)
                                }
                                MedicineFrequency.EveryOtherDay -> {
                                    medicinesViewModel.insertMedicines(getAlarmsList(2L, workerID, null, true))
                                    createAlarmItemAndSchedule(requireActivity().applicationContext, 2L)
                                }
                                MedicineFrequency.SpecificDaysOfWeek -> {
                                    medicinesViewModel.insertMedicines(getAlarmsListForSpecificDays(workerID, null, true))
                                    createAlarmItemAndSchedule(requireActivity().applicationContext)
                                }
                                MedicineFrequency.EveryXDays, MedicineFrequency.EveryXWeeks, MedicineFrequency.EveryXMonths -> {
                                    medicinesViewModel.insertMedicines(getAlarmsList(interval, workerID, null, true))
                                    createAlarmItemAndSchedule(requireActivity().applicationContext, interval)
                                }
                            }

                            setNumberOfAlarms(1)
                            clearTreatmentPeriod()
                            clearAlarmArray()

                            Toast.makeText(requireContext(),
                                getString(R.string.alarms_successfully_created),
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
            userWillSetPeriod()

            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.select_the_treatment_duration))
                .build()

            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                // Handle the selected date range
                val startDateMillis = selection.first
                val endDateMillis = selection.second

                val getInterval = sharedViewModel.getInterval().toLong()

                //Extracts the treatment period and adds the user alarm hour to it in milliseconds
                extractDateComponents(startDateMillis, endDateMillis, true)

                when (getMedicineFrequency()){
                    MedicineFrequency.EveryDay -> {
                        medicinesViewModel.insertMedicines(getAlarmsList(1L, null, true))
                        createAlarmItemAndSchedule(requireActivity().applicationContext, 1L)
                    }
                    MedicineFrequency.EveryOtherDay -> {
                        medicinesViewModel.insertMedicines(getAlarmsList(2L, null, true))
                        createAlarmItemAndSchedule(requireActivity().applicationContext, 2L)
                    }
                    MedicineFrequency.SpecificDaysOfWeek -> {
                        medicinesViewModel.insertMedicines(getAlarmsListForSpecificDays(null, true))
                        createAlarmItemAndSchedule(requireActivity().applicationContext)
                    }
                    MedicineFrequency.EveryXDays, MedicineFrequency.EveryXWeeks, MedicineFrequency.EveryXMonths -> {
                        medicinesViewModel.insertMedicines(getAlarmsList(getInterval, null, true))
                        createAlarmItemAndSchedule(requireActivity().applicationContext, getInterval)
                    }
                }

                setNumberOfAlarms(1)
                clearTreatmentPeriod()
                clearAlarmArray()

                Toast.makeText(requireContext(),
                    getString(R.string.alarms_successfully_created),
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

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}
