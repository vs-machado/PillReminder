package com.phoenix.pillreminder.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class TreatmentDurationFragment : Fragment() {
    private lateinit var binding: FragmentTreatmentDurationBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

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
    ): View? {
        binding = FragmentTreatmentDurationBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.apply{
            toolbar.setupWithNavController(navController, appBarConfiguration)

            // Med forms list. User must select the desired type of med
            val list: MutableList<String> = mutableListOf("Yes, I do", "No, I don't")
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
            lvTreatmentDuration.adapter = arrayAdapter

            lvTreatmentDuration.setOnItemClickListener { _, it, position, _ ->
                when (position){
                    0 -> {
                        showDateRangePicker()

                        /*Toast.makeText(requireContext(),
                            "Alarms successfully created!",
                            Toast.LENGTH_LONG).show()
                        it.findNavController().navigate(R.id.action_treatmentDurationFragment_to_homeFragment)*/
                    }
                    1 -> {
                        //setAlarms for an indefinite period
                        Toast.makeText(requireContext(),
                            "Alarms successfully created!",
                            Toast.LENGTH_LONG).show()
                        it.findNavController().navigate(R.id.action_treatmentDurationFragment_to_homeFragment)
                    }
                }

            }

        }
    }

    private fun showDateRangePicker(){
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select the treatment duration:")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener {
            @Override
            fun onPositiveButtonClick(selection: Pair<Long, Long>){
                val startDateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(selection.first)
                val endDateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(selection.second)

            }
        }

        dateRangePicker.show(childFragmentManager, "DATE_RANGE_PICKER")

    }
}
