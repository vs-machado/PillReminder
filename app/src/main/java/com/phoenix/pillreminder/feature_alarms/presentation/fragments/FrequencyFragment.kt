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
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentFrequencyBinding
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class FrequencyFragment : Fragment() {
    private lateinit var binding:FragmentFrequencyBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFrequencyBinding.inflate(layoutInflater)

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
                findNavController().navigate(R.id.action_frequencyFragment_to_howManyPerDayFragment)
            }

        }

    }
    private fun checkSelectedOption(position: Int){
        when (position){
            0 -> {
                sharedViewModel.setMedicineFrequency(1)
            }

            1 -> {
                sharedViewModel.setMedicineFrequency(2)
            }

            2 -> {
                // Needs to navigate to another fragment and receive user input
                //Needs to calculate how many days has in the period
                sharedViewModel.setMedicineFrequency(1) // Temporarily not working
            }

            3 -> {
                // Needs to navigate to another fragment and receive user input
                sharedViewModel.setMedicineFrequency(1) // Temporarily not working
            }

            4 -> {
                // Needs to navigate to another fragment and receive user input
                //Needs to calculate how many days has in the period
                sharedViewModel.setMedicineFrequency(1) // Temporarily not working
            }

            5 -> {
                //Needs to navigate to another fragment and receive user input
                //Needs to calculate how many days has in the period
                sharedViewModel.setMedicineFrequency(1) // Temporarily not working
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