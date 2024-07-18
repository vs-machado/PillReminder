package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentEveryXPeriodBinding
import com.phoenix.pillreminder.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

class EveryXPeriodFragment: Fragment() {
    private lateinit var binding: FragmentEveryXPeriodBinding
    private val sharedViewModel: AlarmSettingsSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEveryXPeriodBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply{
            val interval = etInterval.text

            try{
                when(sharedViewModel.getMedicineFrequency()){
                    MedicineFrequency.EveryXDays -> tvPeriod.text = "days"
                    MedicineFrequency.EveryXWeeks -> tvPeriod.text = "weeks"
                    MedicineFrequency.EveryXMonths -> tvPeriod.text = "months"
                    else -> {throw IllegalArgumentException("Invalid medicine frequency") }
                }
            } catch (e: IllegalArgumentException){
                Log.e("PillReminder", "Illegal argument provided in EveryXPeriodFragment", e)
                Toast.makeText(context, "An error occurred. Please, try again.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }

            etInterval.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(interval: CharSequence?, start: Int, before: Int, count: Int) {
                    if(!interval.isNullOrBlank()) fabNext.visibility = View.VISIBLE else View.INVISIBLE
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            fabNext.setOnClickListener {
                sharedViewModel.setInterval(interval.toString().toInt())
                findNavController().navigate(R.id.action_everyXPeriodFragment_to_howManyPerDayFragment)
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