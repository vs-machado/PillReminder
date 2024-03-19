package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentFrequencyBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel

class FrequencyFragment : Fragment() {
    private lateinit var binding:FragmentFrequencyBinding
    private val sharedViewModel:AlarmSettingsSharedViewModel by activityViewModels()

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
            val list: MutableList<String> = mutableListOf("Every day", "Every other day", "Specific days of the week", "Every X days",
                "Every X weeks", "Every X months")
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
                sharedViewModel.setMedicineFrequency("Every day")
            }

            1 -> {
                sharedViewModel.setMedicineFrequency("Every other day")
            }

            2 -> {
                sharedViewModel.setMedicineFrequency("Specific days of the week")
            }

            3 -> {
                sharedViewModel.setMedicineFrequency("Every X days")
            }

            4 -> {
                sharedViewModel.setMedicineFrequency("Every X weeks")
            }

            5 -> {
                sharedViewModel.setMedicineFrequency("Every X months")
            }
        }
    }
}