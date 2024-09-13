package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentFrequencyBinding
import com.phoenix.pillreminder.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.pillreminder.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class FrequencyFragment : Fragment() {
    private lateinit var binding:FragmentFrequencyBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFrequencyBinding.inflate(layoutInflater)

        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.colorPrimary,
            R.color.white,
            R.color.dark_gray,
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
        super.onViewCreated(view, savedInstanceState)

        binding.apply{
            val navController = findNavController()
            val appBarConfiguration = AppBarConfiguration(navController.graph)

            toolbarFrequency.setupWithNavController(navController, appBarConfiguration)

            // Frequency of med usage list. User must select the desired usage interval
            val list: MutableList<String> = mutableListOf(
                getString(R.string.every_day),
                getString(R.string.every_other_day),
                getString(R.string.specific_days_of_the_week), getString(R.string.every_x_days),
                getString(R.string.every_x_weeks), getString(R.string.every_x_months))
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvFrequency.adapter = arrayAdapter

            // Check the user selected option and navigate to the next fragment
            lvFrequency.setOnItemClickListener { _, _, position, _ ->
                checkSelectedOption(position)

                when(sharedViewModel.getMedicineFrequency()){
                    MedicineFrequency.EveryDay, MedicineFrequency.EveryOtherDay -> {
                        findNavController().navigate(R.id.action_frequencyFragment_to_howManyPerDayFragment)
                    }
                    MedicineFrequency.SpecificDaysOfWeek -> {
                        findNavController().navigate(R.id.action_frequencyFragment_to_dayPickerFragment)
                    }
                    MedicineFrequency.EveryXDays, MedicineFrequency.EveryXWeeks, MedicineFrequency.EveryXMonths -> {
                        findNavController().navigate(R.id.action_frequencyFragment_to_everyXPeriodFragment)
                    }
                }
            }

        }

    }
    private fun checkSelectedOption(position: Int){
        sharedViewModel.apply{
            when (position){
                0 -> setMedicineFrequency(MedicineFrequency.EveryDay)
                1 -> setMedicineFrequency(MedicineFrequency.EveryOtherDay)
                2 -> setMedicineFrequency(MedicineFrequency.SpecificDaysOfWeek)
                3 -> setMedicineFrequency(MedicineFrequency.EveryXDays)
                4 -> setMedicineFrequency(MedicineFrequency.EveryXWeeks)
                5 -> setMedicineFrequency(MedicineFrequency.EveryXMonths)
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
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