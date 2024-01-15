package com.phoenix.pillreminder.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.phoenix.pillreminder.databinding.FragmentAlarmHourBinding
import com.phoenix.pillreminder.model.AlarmSettingsSharedViewModel

class AlarmHourFragment : Fragment() {
    private lateinit var binding: FragmentAlarmHourBinding
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

        val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()
        var currentAlarmNumber = sharedViewModel.currentAlarmNumber.value
        val numberOfAlarms = sharedViewModel.numberOfAlarms

        sharedViewModel.currentAlarmNumber.observe(viewLifecycleOwner, Observer{
            setTvAlarmHour(it)
        })

        //Asks the user the next alarm hour
        binding.fabNext.setOnClickListener {
            sharedViewModel.updateCurrentAlarmNumber()
        }
    }

    private fun setTvAlarmHour(currentAlarmNumber: Int){
        when(currentAlarmNumber){
            2 -> {
                binding.tvAlarmHour.text = "Please, set the second medicine alarm:"
            }
            3 -> {
                binding.tvAlarmHour.text = "Please, set the third medicine alarm:"
            }
            4 -> {
                binding.tvAlarmHour.text = "Please, set the fourth medicine alarm:"
            }
            5 -> {
                binding.tvAlarmHour.text = "Please, set the fifth medicine alarm:"
            }
            6 -> {
                binding.tvAlarmHour.text = "Please, set the sixth medicine alarm:"
            }
            7 -> {
                binding.tvAlarmHour.text = "Please, set the seventh medicine alarm:"
            }
            8 -> {
                binding.tvAlarmHour.text = "Please, set the eighth medicine alarm:"
            }
            9 -> {
                binding.tvAlarmHour.text = "Please, set the ninth medicine alarm:"
            }
            10 -> {
                binding.tvAlarmHour.text = "Please, set the tenth medicine alarm:"
            }
        }
    }

}