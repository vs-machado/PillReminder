package com.phoenix.pillreminder.fragments

import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentAlarmHourBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel
import java.util.Calendar

class AlarmHourFragment : Fragment() {
    private lateinit var binding: FragmentAlarmHourBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (sharedViewModel.currentAlarmNumber.value == 1) {
                findNavController().popBackStack()
            } else {
                //If the user goes back to the previous alarm, the position is decreased by 1
                sharedViewModel.position--
                sharedViewModel.decreaseCurrentAlarmNumber()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmHourBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val hourFormat = is24HourFormat(requireContext())

        sharedViewModel.currentAlarmNumber.observe(viewLifecycleOwner, Observer {
            //Sets the text of the tvAlarmHour according to the current alarm number and goes to the next position of the alarm array.
            setTvAlarmHourAndPosition(it)
        })


        binding.apply {
            sharedViewModel.apply {
                toolbar.setupWithNavController(navController, appBarConfiguration)

                tpAlarm.setIs24HourView(hourFormat)

                tpAlarm.setOnTimeChangedListener { _, hourOfDay, minute ->
                    saveAlarmHour(position, hourOfDay, minute)
                }

                //Asks the user the next alarm hour
                fabNext.setOnClickListener {
                    updateCurrentAlarmNumber()

                    if (currentAlarmNumber.value!! > numberOfAlarms.value!!) {
                        //Clear the remaining positions of the alarm array
                        clearAlarmArray()
                        findNavController().navigate(R.id.action_alarmHourFragment_to_treatmentDurationFragment)
                    }
                }
            }
        }
    }


    private fun setTvAlarmHourAndPosition(currentAlarmNumber: Int) {
        when (currentAlarmNumber) {
            1 -> {
                binding.tvAlarmHour.text = "Please, set the medicine alarm hour:"
            }

            2 -> {
                binding.tvAlarmHour.text = "Please, set the second medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            3 -> {
                binding.tvAlarmHour.text = "Please, set the third medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            4 -> {
                binding.tvAlarmHour.text = "Please, set the fourth medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            5 -> {
                binding.tvAlarmHour.text = "Please, set the fifth medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            6 -> {
                binding.tvAlarmHour.text = "Please, set the sixth medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            7 -> {
                binding.tvAlarmHour.text = "Please, set the seventh medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            8 -> {
                binding.tvAlarmHour.text = "Please, set the eighth medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            9 -> {
                binding.tvAlarmHour.text = "Please, set the ninth medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }

            10 -> {
                binding.tvAlarmHour.text = "Please, set the tenth medicine alarm:"
                sharedViewModel.position++
                resetTimePicker(binding.tpAlarm)
            }
    }
}

    private fun resetTimePicker(timePicker: TimePicker) {
        timePicker.hour = currentHour
        timePicker.minute = currentMinute
    }
}