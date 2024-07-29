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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentEveryXPeriodBinding
import com.phoenix.pillreminder.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

//Catches the interval of the medicine
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
            try{
                when(sharedViewModel.getMedicineFrequency()){
                    MedicineFrequency.EveryXDays -> tvPeriod.text = context?.getString(R.string.days)
                    MedicineFrequency.EveryXWeeks -> tvPeriod.text = context?.getString(R.string.weeks)
                    MedicineFrequency.EveryXMonths -> tvPeriod.text = context?.getString(R.string.months)
                    else -> {throw IllegalArgumentException("Invalid medicine frequency") }
                }
            } catch (e: IllegalArgumentException){
                Log.e("PillReminder", "Illegal argument provided in EveryXPeriodFragment", e)
                Toast.makeText(context, "An error occurred. Please, try again.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }

            // Hides fab button if the interval field is empty
            etInterval.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(interval: CharSequence?, start: Int, before: Int, count: Int) {
                    val inputIsFilled = interval?.isNotBlank() ?: false
                    fabNext.visibility = if(inputIsFilled) View.VISIBLE else View.INVISIBLE
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Saves the interval and navigates to the next fragment
            fabNext.setOnClickListener {
                val interval = etInterval.text

                try{
                    val intervalInt = interval.toString().toInt()

                    if(intervalInt == 0){
                        Toast.makeText(
                            context,
                            "Interval cannot be zero. Please enter a valid interval.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    sharedViewModel.setInterval(intervalInt)
                    findNavController().navigate(R.id.action_everyXPeriodFragment_to_howManyPerDayFragment)

                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        context,
                        "Invalid input. Please enter a valid number.",
                        Toast.LENGTH_LONG
                    ).show()
                }
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