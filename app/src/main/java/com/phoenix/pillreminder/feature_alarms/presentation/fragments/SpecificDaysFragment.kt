package com.phoenix.pillreminder.feature_alarms.presentation.fragments

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
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentSpecificDaysBinding
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.DayPickerAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class SpecificDaysFragment : Fragment() {

    private lateinit var binding: FragmentSpecificDaysBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSpecificDaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        val list: MutableList<String> = mutableListOf(
            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday"
        )
        val arrayAdapter = DayPickerAdapter(requireContext(), list)
        binding.lvDayPicker.adapter = arrayAdapter

        binding.lvDayPicker.setOnItemClickListener{ _,_, position, _ ->
            val arrayNotEmpty = arrayAdapter.checkItemSelection(position)
            binding.fabNext.visibility = if(arrayNotEmpty) View.VISIBLE else View.INVISIBLE
            sharedViewModel.setSelectedDaysList(arrayAdapter.getSelectedDaysList())
        }

        binding.fabNext.setOnClickListener {
            findNavController().navigate(R.id.action_specificDaysFragment_to_howManyPerDayFragment)
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