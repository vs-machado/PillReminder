package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentSpecificDaysBinding
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.DayPickerAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

// Used to specify the days of the week in which the medicine should be taken.
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

        val list = listOf(
            R.string.Sunday,
            R.string.Monday,
            R.string.Tuesday,
            R.string.Wednesday,
            R.string.Thursday,
            R.string.Friday,
            R.string.Saturday
        ).map { requireContext().getString(it) }

        val arrayAdapter = DayPickerAdapter(
            requireContext(),
            list,
            ContextCompat.getColor(requireContext(), R.color.white_ice)
        )
        binding.lvDayPicker.adapter = arrayAdapter

        // The selected days array list is 1 based. Position adjustment made in checkItemSelection method.
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