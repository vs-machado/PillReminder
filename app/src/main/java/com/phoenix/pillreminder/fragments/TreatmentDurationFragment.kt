package com.phoenix.pillreminder.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentTreatmentDurationBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel

class TreatmentDurationFragment : Fragment() {
    private lateinit var binding: FragmentTreatmentDurationBinding
    val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            sharedViewModel.position--
            sharedViewModel.decreaseCurrentAlarmNumber()
            findNavController().navigate(R.id.action_treatmentDurationFragment_to_alarmHourFragment)
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


        }
    }
}
