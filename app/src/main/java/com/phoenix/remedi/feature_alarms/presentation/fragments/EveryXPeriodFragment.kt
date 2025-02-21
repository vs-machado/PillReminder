package com.phoenix.remedi.feature_alarms.presentation.fragments

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
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.FragmentEveryXPeriodBinding
import com.phoenix.remedi.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.remedi.feature_alarms.presentation.OnOneOffClickListener
import com.phoenix.remedi.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.apply{
            toolbar.setupWithNavController(navController, appBarConfiguration)

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
            fabNext.setOnClickListener(object: OnOneOffClickListener() {
                override fun onSingleClick(fab: FloatingActionButton) {
                    val interval = etInterval.text

                    try{
                        val intervalInt = interval.toString().toInt()

                        if(intervalInt == 0){
                            Toast.makeText(
                                context,
                                "Interval cannot be zero. Please enter a valid interval.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
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
            })
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