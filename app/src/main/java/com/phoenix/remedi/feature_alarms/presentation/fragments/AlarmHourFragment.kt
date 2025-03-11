package com.phoenix.remedi.feature_alarms.presentation.fragments

import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.FragmentAlarmHourBinding
import com.phoenix.remedi.feature_alarms.domain.model.Animation
import com.phoenix.remedi.feature_alarms.presentation.OnOneOffClickListener
import com.phoenix.remedi.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.AlarmHourViewModel
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Allow the user to select the alarm hour using a TimePicker and handle the input accordingly.
 * The quantity of alarm hours will be determined by [AlarmSettingsSharedViewModel.setNumberOfAlarms] parameter value
 * passed on [HowManyPerDayFragment] by the user.
 * There's two variables for tracking the alarm position to be saved. [AlarmSettingsSharedViewModel.alarmIndex]
 * is used to define the position of the alarm on the array to be stored in a variable.
 * [AlarmSettingsSharedViewModel.currentAlarmNumber] is used to define the tvAlarmHour text displayed to the user,
 * indicating which alarm the user is currently setting.
 */
class AlarmHourFragment : Fragment() {
    private lateinit var binding: FragmentAlarmHourBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
    private val alarmHourViewModel: AlarmHourViewModel by viewModels()
    private val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, true) {
            handleBackPressed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmHourBinding.inflate(layoutInflater)

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
        val hourFormat = is24HourFormat(requireContext())

        sharedViewModel.currentAlarmNumber.observe(viewLifecycleOwner) {
            //Sets the text of the tvAlarmHour according to the current alarm number and goes to the next position of the alarm array.
            setTvAlarmHourAndPosition(it)
        }


        binding.apply {
            sharedViewModel.apply {
                toolbar.setupWithNavController(navController, appBarConfiguration)
                toolbar.setNavigationOnClickListener {
                    handleBackPressed()
                }

                // Checks the user's phone hour format and sets the TimePicker
                tpAlarm.setIs24HourView(hourFormat)

                // Saves the alarm hour if user does not change the time on TimePicker
                alarmHourViewModel.apply{
                    saveAlarmHour(alarmIndex, getCurrentHour(), getCurrentMinute())
                }

                tpAlarm.setOnTimeChangedListener { _, hourOfDay, minute ->
                    if (alarmIndex < getAlarmsPerDay()) {
                        saveAlarmHour(alarmIndex, hourOfDay, minute)
                    }
                }

                // Asks the user the next alarm hour. The custom click listener is used to prevent fast tapping.
                // Fast tapping can break the quantity of alarms being set.
                fabNext.setOnClickListener(object: OnOneOffClickListener() {

                    override fun onSingleClick(fab: FloatingActionButton) {
                        // Add touch feedback
                        fab.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

                        sharedViewModel.setAnimation(Animation.ENABLED)
                        sharedViewModel.alarmIndex++
                        updateCurrentAlarmNumber()

                        if (currentAlarmNumber.value!! > getAlarmsPerDay()) {
                            //Clear the remaining positions of the alarm array
                            clearRemainingAlarmArrayPositions()
                            findNavController().navigate(R.id.action_alarmHourFragment_to_treatmentDurationFragment)
                        }
                    }
                })
            }
        }
    }

    // Allow users to navigate back to the previous alarm setting or to the previous fragment.
    private fun handleBackPressed() {
        if (sharedViewModel.currentAlarmNumber.value == 1) {
            sharedViewModel.setAnimation(Animation.DISABLED)
            sharedViewModel.setNumberOfAlarms(1)
            findNavController().popBackStack()
        } else {
            // If the user goes back to the previous alarm, the position is decreased by 1
            // and the tvAlarmHour text changes.
            sharedViewModel.alarmIndex--
            sharedViewModel.decreaseCurrentAlarmNumber()

            if(sharedViewModel.animation.value == Animation.DISABLED) {
                sharedViewModel.setAnimation(Animation.ENABLED)
            }
        }
    }

    private fun setTvAlarmHourAndPosition(currentAlarmNumber: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                sharedViewModel.animation.collect { animation ->
                    // Skip animation only when it's the first alarm AND we're moving forward (initial setup)
                    if (animation == Animation.DISABLED) {
                        setAlarmText(currentAlarmNumber)
                    } else {
                        if(animation == Animation.ENABLED) {
                            binding.tvAlarmHour.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    setAlarmText(currentAlarmNumber)

                                    binding.tvAlarmHour.animate()
                                        .alpha(1f)
                                        .setDuration(200)
                                        .start()
                                }
                                .start()
                      }
                    }
                }
            }
        }
    }

    private fun setAlarmText(currentAlarmNumber: Int) {
        when (currentAlarmNumber) {
            1 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_medicine_alarm_hour)
            }
            2 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_second_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            3 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_third_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            4 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_fourth_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            5 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_fifth_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            6 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_sixth_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            7 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_seventh_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            8 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_eighth_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            9 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_ninth_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }

            10 -> {
                binding.tvAlarmHour.text = getString(R.string.please_set_the_tenth_medicine_alarm)
                resetTimePicker(binding.tpAlarm)
            }
        }
    }

    // Resets the TimePicker after setting each alarm
    private fun resetTimePicker(timePicker: TimePicker) {
        timePicker.hour = currentHour
        timePicker.minute = currentMinute
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