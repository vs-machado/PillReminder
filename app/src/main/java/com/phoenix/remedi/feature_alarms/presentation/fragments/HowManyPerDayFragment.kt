package com.phoenix.remedi.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.FragmentHowManyPerDayBinding
import com.phoenix.remedi.feature_alarms.presentation.OnOneOffClickListener
import com.phoenix.remedi.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class HowManyPerDayFragment : Fragment() {
    private lateinit var binding: FragmentHowManyPerDayBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHowManyPerDayBinding.inflate(layoutInflater)

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
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

        binding.apply{
            toolbarHowMany.setupWithNavController(navController, appBarConfiguration)
            setNumberPicker()

            npHowOften.setOnValueChangedListener { _, _, newVal ->
                //Fixes position values
                sharedViewModel.setNumberOfAlarms(newVal + 1)
            }

            fabNext.setOnClickListener(object: OnOneOffClickListener() {
                override fun onSingleClick(fab: FloatingActionButton) {
                    sharedViewModel.position = 0
                    sharedViewModel.resetCurrentAlarmNumber()
                    findNavController().navigate(R.id.action_howManyPerDayFragment_to_alarmHourFragment)
                }
            })
        }

    }

    private fun setNumberPicker(){
        binding.apply{
            // Sets doses options
            npHowOften.minValue = 0
            npHowOften.maxValue = 9
            npHowOften.textSize = 100F

            val pickerVals = intArrayOf(1,2,3,4,5,6,7,8,9,10)
            val stringPickerVals = pickerVals.map{it.toString()}.toTypedArray()

            npHowOften.displayedValues = stringPickerVals
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