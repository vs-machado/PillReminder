package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentMedicineDetailsBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.presentation.utils.DateUtil
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MedicineDetailsFragment: Fragment() {
    private lateinit var binding: FragmentMedicineDetailsBinding
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicineDetailsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicine = arguments?.getParcelable("medicine", Medicine::class.java)

        binding.apply{
            tvNameMed.text = medicine?.name

            when(medicine?.form){
                "pill" -> {
                    imageView.setImageResource(R.drawable.ic_pill_coloured)
                    tvQuantityMed.text = context?.resources?.getQuantityString(R.plurals.pills_quantity, medicine.quantity.toInt(), medicine.quantity.toInt())
                }
                "drop" -> {
                    imageView.setImageResource(R.drawable.ic_dropper)
                    tvQuantityMed.text = context?.resources?.getQuantityString(R.plurals.drops_quantity, medicine.quantity.toInt(), medicine.quantity.toInt())
                }
                "pomade" -> {
                    imageView.setImageResource(R.drawable.ic_ointment)
                    tvQuantityMed.text = context?.getString(R.string.number_application)
                }
                "injection" -> {
                    imageView.setImageResource(R.drawable.ic_injection)
                    when(medicine.unit){
                        "syringe" ->  {
                            val quantity = medicine.quantity.toInt()
                            tvQuantityMed.text = context?.resources?.getQuantityString(R.plurals.syringe_quantity, quantity, quantity)
                        }
                        "mL" -> tvQuantityMed.text = context?.getString(R.string.number_ml, medicine.quantity)
                    }
                }
                "liquid" -> {
                    imageView.setImageResource(R.drawable.ic_liquid)
                    tvQuantityMed.text = context?.getString(R.string.number_ml, medicine.quantity)
                }
                "inhaler" -> {
                    imageView.setImageResource(R.drawable.ic_inhalator)
                    when(medicine.unit){
                        "mg" -> tvQuantityMed.text = context?.getString(R.string.quantity_inhalator, medicine.quantity)
                        "puff" -> tvQuantityMed.text = context?.resources?.getQuantityString(R.plurals.puffs_quantity, medicine.quantity.toInt(), medicine.quantity.toInt())
                        "mL" -> tvQuantityMed.text = context?.getString(R.string.quantity_ml, medicine.quantity)
                    }
                }
            }

            when(medicine?.medicineFrequency){
                "EveryDay" -> tvFrequencyMed.text = context?.getString(R.string.every_day)
                "SpecificDaysOfWeek" -> {
                    val daysOfWeek = listOf(
                        context?.getString(R.string.Sunday),
                        context?.getString(R.string.Monday),
                        context?.getString(R.string.Tuesday),
                        context?.getString(R.string.Wednesday),
                        context?.getString(R.string.Thursday),
                        context?.getString(R.string.Friday),
                        context?.getString(R.string.Saturday),
                    )

                    // Convert the mutableSet to a sorted days of week list and join it with commas
                    val selectedDays = medicine.selectedDaysOfWeek?.sorted()?.joinToString(separator = ", ") { daysOfWeek[it - 1] ?: "" }
                    tvFrequencyMed.text = context?.getString(R.string.specific_days_of_week, selectedDays)
                }
                "EveryOtherDay" -> tvFrequencyMed.text = context?.getString(R.string.every_other_day)
                "EveryXDays" -> tvFrequencyMed.text = context?.getString(R.string.every_interval_days, medicine.interval.toInt())
                "EveryXWeeks" -> tvFrequencyMed.text = context?.getString(R.string.every_interval_weeks, medicine.interval.toInt())
                "EveryXMonths" -> tvFrequencyMed.text = context?.getString(R.string.every_interval_months, medicine.interval.toInt())
                else -> tvFrequencyMed.text = ""
            }

            tvAlarmsQuantity.text = context?.getString(R.string.quantity_alarms, medicine?.alarmsPerDay)

            if(medicine != null) {
                tvTreatmentStartDate.text = DateUtil.millisToDateString("", medicine.startDate)
                tvTreatmentEndDate.text = DateUtil.millisToDateString("", medicine.endDate)

                tvTreatmentStatus.text = when {
                    System.currentTimeMillis() < medicine.startDate && medicine.isActive -> context?.getString(R.string.hasn_started)
                    System.currentTimeMillis() <= medicine.endDate && medicine.isActive -> context?.getString(R.string.ongoing)
                    else -> context?.getString(R.string.ended)
                }

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                    val result = withContext(Dispatchers.IO){
                        val cutoffTime = medicinesViewModel.getMedicineEditTimestamp(medicine.name)
                        medicinesViewModel.getAlarmTimesForMedicine(medicine.name, cutoffTime, medicine.treatmentID)
                    }

                    // Concatenates all the different alarm hours for the same medicine in a string
                    tvAlarmsHour.text = result.joinToString(", ")
                }
            }


        }
    }

}