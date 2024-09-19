package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentEditMedicinesBinding
import com.phoenix.pillreminder.databinding.LayoutEveryXPeriodDialogBinding
import com.phoenix.pillreminder.databinding.LayoutSpecificDaysDialogBinding
import com.phoenix.pillreminder.databinding.LayoutTimepickerDialogBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.AlarmsHourListAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.DayPickerAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.NoFilterAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.utils.DateUtil
import com.phoenix.pillreminder.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.EditMedicinesViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

/* Class used to edit the medicines data. It allows the change of the medicine name,
   form, quantity, dose unit, treatment end date, frequency and the alarms hour. */
@AndroidEntryPoint
class EditMedicinesFragment: Fragment() {
    private lateinit var binding: FragmentEditMedicinesBinding
    private val editMedicinesViewModel: EditMedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val alarmSettingsSharedViewModel: AlarmSettingsSharedViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: AlarmsHourListAdapter
    private var alarmHourList: List<AlarmHour>? = null
    private lateinit var millisList: List<Long>
    private var endDateMillis: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditMedicinesBinding.inflate(layoutInflater)
        val medicine = arguments?.getParcelable("edit_medicine", Medicine::class.java)
        medicine?.let { setupAdapters(it) }

        hideFabAndBottomNav()
        setupViewTheme()
        setupToolbar()

        return binding.root
    }

    // All variables must be initialized when user navigates to this fragment to avoid issues when pressing Save button

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSave = requireActivity().findViewById<TextView>(R.id.tvSave)
        tvSave.visibility = View.VISIBLE

        val medicine = arguments?.getParcelable("edit_medicine", Medicine::class.java)

        binding.apply{
            if(medicine != null) {
                when(medicine.medicinePeriodSet){
                    true -> {
                        // Programmatically sets the calendar icon color to black
                        val drawableEndDate = ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar_month_24)
                        drawableEndDate?.setTint(ContextCompat.getColor(requireContext(), R.color.black))
                        val drawableStartDate = ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar_month_24)
                        drawableStartDate?.setTint(Color.TRANSPARENT)

                        tietStartDate.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableStartDate, null)
                        tietEndDate.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableEndDate, null)
                    }
                    false -> {
                        val drawableStartDate = ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar_month_24)
                        drawableStartDate?.setTint(Color.TRANSPARENT)
                        tietStartDate.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableStartDate, null)

                        // Hide the icon. User can't change the end date because no treatment period was defined.
                        tietEndDate.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                    }
                }

                // Fill the text inputs with the medicine data
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                    initRecyclerView(medicine)
                }



                val startDate = DateUtil.millisToDateString(null, medicine.startDate)
                var endDate = DateUtil.millisToDateString(null, medicine.endDate)

                // Initializes medicine name, quantity, form, interval, treatment end date and sets the text
                tietMedicineName.setText(medicine.name)
                tietQuantity.setText(medicine.quantity.toString())
                tvQuantityAlarms.text = context?.getString(R.string.alarms_per_day_details, medicine.alarmsPerDay)
                tietStartDate.setText(startDate)

                alarmSettingsSharedViewModel.setInterval(medicine.interval.toInt())
                alarmSettingsSharedViewModel.setTreatmentStartDate(medicine.startDate)
                alarmSettingsSharedViewModel.setTreatmentEndDate(medicine.endDate)
                alarmSettingsSharedViewModel.setDoseUnit(medicine.unit)
                alarmSettingsSharedViewModel.setMedicineForm(medicine.form)


                if(medicine.medicinePeriodSet){
                    tietEndDate.setText(endDate)
                    tietEndDate.keyListener = null

                    // User wants to change treatment period
                    tietEndDate.setOnClickListener {
                        val datePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText(context?.getString(R.string.select_treatment_end_date))
                            .build()

                        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
                            // Resets the selected date millis to 00:00, needed to compare user
                            // selected end date with start date
                            val userTimeZone = TimeZone.getDefault()
                            val selectedDate = editMedicinesViewModel.resetCalendarHourToMidnight(selectedDateMillis, userTimeZone)
                            val startDateMidnight = editMedicinesViewModel.resetCalendarHourToMidnight(medicine.startDate, userTimeZone)

                            if(selectedDate < startDateMidnight){
                                Toast.makeText(
                                    requireContext(),
                                    context?.getString(R.string.treatment_end_date_error),
                                    Toast.LENGTH_LONG
                                ).show()

                                return@addOnPositiveButtonClickListener
                            }
                            endDate = DateUtil.millisToDateString(null, selectedDate)
                            tietEndDate.setText(endDate)

                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                val lastAlarmOfTheDayMillis = medicinesViewModel.getAlarmTimeSinceMidnight(medicine.name)

                                // Used to set the treatment end date.
                                endDateMillis = editMedicinesViewModel.formatSelectedDateWithOffset(selectedDateMillis, lastAlarmOfTheDayMillis, userTimeZone)
                                alarmSettingsSharedViewModel.setTreatmentEndDate(endDateMillis)
                            }
                        }

                        datePicker.show(childFragmentManager, "DATE_PICKER")
                    }

                } else {
                    tietEndDate.setText(context?.getString(R.string.undefined))
                    tietEndDate.isEnabled = false
                }
            }

            // Initializes medicine form and sets the text
            when(medicine?.form){
                "pill" -> {
                    acTvMedicineForm.setText(context?.getString(R.string.pill), false)
                    tvStrength.text = requireContext().getString(R.string.pills)
                }
                "drop" -> {
                    acTvMedicineForm.setText(context?.getString(R.string.Drops), false)
                    tvStrength.text = context?.getString(R.string.drops)
                }
                "pomade" -> {
                    acTvMedicineForm.setText(context?.getString(R.string.pomade), false)
                    tvStrength.text = requireContext().getString(R.string.application)
                }
                "injection" -> {
                    acTvMedicineForm.setText(context?.getString(R.string.injection), false)
                    tvStrength.text = context?.getString(R.string.mls)
                }
                "liquid" -> {
                    acTvMedicineForm.setText(context?.getString(R.string.liquid), false)
                    tvStrength.text = context?.getString(R.string.mls)
                }
                "inhaler" -> {
                    acTvMedicineForm.setText(context?.getString(R.string.inhaler), false)
                    tvStrength.text = context?.getString(R.string.mgs)
                }
            }

            // Initializes medicine frequency and sets the text
            when(medicine?.medicineFrequency){
                "EveryDay" -> acTvMedicineFrequency.setText(context?.getString(R.string.every_day), false)
                "EveryOtherDay" -> acTvMedicineFrequency.setText(context?.getString(R.string.every_other_day), false)
                "SpecificDaysOfWeek" -> acTvMedicineFrequency.setText(context?.getString(R.string.specific_days_of_the_week), false)
                "EveryXDays" -> acTvMedicineFrequency.setText(context?.getString(R.string.every_x_days), false)
                "EveryXWeeks" -> acTvMedicineFrequency.setText(context?.getString(R.string.every_x_weeks), false)
                "EveryXMonths" -> acTvMedicineFrequency.setText(context?.getString(R.string.every_x_months), false)
            }

            acTvMedicineForm.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                tietQuantity.isEnabled = true

                when(selectedItem){
                    context?.getString(R.string.pomade) -> {
                        tietQuantity.isEnabled = false
                        tietQuantity.setText("1")
                        tvStrength.text = ""
                    }
                    context?.getString(R.string.pill) -> tvStrength.text = context?.getString(R.string.pills)
                    context?.getString(R.string.Drops) -> tvStrength.text = context?.getString(R.string.drops)
                    context?.getString(R.string.injection) -> tvStrength.text = context?.getString(R.string.mls)
                    context?.getString(R.string.liquid) -> tvStrength.text = context?.getString(R.string.mls)
                    context?.getString(R.string.inhaler) -> tvStrength.text = context?.getString(R.string.mgs)
                }
            }
        }

        binding.acTvMedicineFrequency.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position).toString()

            // Shows a dialog allowing the user to set a custom interval
            medicine?.let{
                when(selectedItem){
                    context?.getString(R.string.specific_days_of_the_week) -> { showSpecificDaysDialog() }
                    context?.getString(R.string.every_x_days) -> { showPeriodDialog(medicine, "days") }
                    context?.getString(R.string.every_x_weeks) -> { showPeriodDialog(medicine, "weeks") }
                    context?.getString(R.string.every_x_months) -> { showPeriodDialog(medicine, "months") }
                }
            }
        }

        tvSave.setOnClickListener {
            val inputFrequency = binding.acTvMedicineFrequency.text.toString()
            val medicineName = binding.tietMedicineName.text.toString()
            var doseUnit = ""

            val medicineForm = when(binding.acTvMedicineForm.text.toString()){
                context?.getString(R.string.pill) -> "pill"
                context?.getString(R.string.Drops) -> "drop"
                context?.getString(R.string.pomade) -> "pomade"
                context?.getString(R.string.injection) -> "injection"
                context?.getString(R.string.liquid) -> "liquid"
                context?.getString(R.string.inhaler) -> "inhaler"

                else -> throw IllegalArgumentException("Invalid argument")
            }

            if(binding.acTvDoseUnit.isVisible){
                doseUnit = binding.acTvDoseUnit.text.toString()
            } else {
                if (medicine != null) {
                    doseUnit = medicine.unit
                }
            }

            val quantity = binding.tietQuantity.text.toString().toFloat()
            val alarmsPerDay = medicine?.alarmsPerDay // Needs to allow user to change it

            // Medicine save logic
            alarmSettingsSharedViewModel.apply {
                setMedicineName(medicineName)
                setMedicineForm(medicineForm)
                setDoseUnit(doseUnit)
                setMedicineQuantity(quantity)
                setNumberOfAlarms(alarmsPerDay!!)
                setTreatmentID(medicine.treatmentID)

                when(inputFrequency){
                    context?.getString(R.string.every_day) -> setMedicineFrequency(MedicineFrequency.EveryDay)
                    context?.getString(R.string.every_other_day) -> setMedicineFrequency(MedicineFrequency.EveryOtherDay)
                    context?.getString(R.string.specific_days_of_the_week) -> setMedicineFrequency(MedicineFrequency.SpecificDaysOfWeek)
                    context?.getString(R.string.every_x_months) -> setMedicineFrequency(MedicineFrequency.EveryXMonths)
                    context?.getString(R.string.every_x_weeks) -> setMedicineFrequency(MedicineFrequency.EveryXWeeks)
                    context?.getString(R.string.every_x_days) -> setMedicineFrequency(MedicineFrequency.EveryXDays)
                }
            }

            alarmSettingsSharedViewModel.apply {
                val cutoffTime = System.currentTimeMillis()

                // The info will update the medicine name, quantity, form, endDate and frequency in alarms already triggered.
                // Useful to properly display the medicine data in MyMedicinesFragment and MedicineDetailsFragment.
                val expiredMedicinesUpdatedInfo = medicine?.let{ getExpiredMedicinesUpdatedInfo(it) }

                when(medicine?.medicinePeriodSet){
                    true -> {
                        extractDateComponents(medicine.startDate, getTreatmentEndDate(), true)

                        val interval = getInterval().toLong()

                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            if(expiredMedicinesUpdatedInfo != null) {
                                when(inputFrequency){
                                    context?.getString(R.string.every_day) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsList(1L, cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext, 1L)
                                        }
                                    }
                                    context?.getString(R.string.every_other_day) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsList(2L, cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext, 2L)
                                        }
                                    }
                                    context?.getString(R.string.specific_days_of_the_week) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsListForSpecificDays(cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext)
                                        }
                                    }
                                    context?.getString(R.string.every_x_days),
                                    context?.getString(R.string.every_x_weeks),
                                    context?.getString(R.string.every_x_months) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsList(getInterval().toLong(), cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext, interval)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    false -> {
                        setTemporaryPeriod()
                        extractDateComponents(medicine.startDate, getTreatmentEndDate(), false)

                        val workerID = alarmSettingsSharedViewModel.createRescheduleWorker(requireContext())
                        val interval = getInterval().toLong()

                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            if(expiredMedicinesUpdatedInfo != null) {
                                when(inputFrequency){
                                    context?.getString(R.string.every_day) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        alarmSettingsSharedViewModel.cancelWork(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsList(1L, workerID, cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext, 1L)
                                        }
                                    }
                                    context?.getString(R.string.every_other_day) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        alarmSettingsSharedViewModel.cancelWork(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsList(2L, workerID, cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext, 2L)
                                        }
                                    }
                                    context?.getString(R.string.specific_days_of_the_week) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        alarmSettingsSharedViewModel.cancelWork(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsListForSpecificDays(workerID, cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext)
                                        }
                                    }
                                    context?.getString(R.string.every_x_days),
                                    context?.getString(R.string.every_x_weeks),
                                    context?.getString(R.string.every_x_months) -> {
                                        medicinesViewModel.updateExpiredMedicines(expiredMedicinesUpdatedInfo)
                                        medicinesViewModel.removeRemainingAlarms(medicine)
                                        alarmSettingsSharedViewModel.cancelWork(medicine)
                                        editMedicinesViewModel.cancelAlarm(medicine, true)
                                        medicinesViewModel.insertMedicines(getAlarmsList(interval, workerID, cutoffTime, false))
                                        withContext(Dispatchers.Default){
                                            createAlarmItemAndSchedule(requireActivity().applicationContext, interval)
                                        }
                                    }
                                }
                            }
                        }

                    }

                    else -> {}
                }
            }

            findNavController().navigateUp()
            Toast.makeText(
                requireContext(),
                context?.getString(R.string.changes_saved),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private suspend fun initRecyclerView(medicine: Medicine){
        binding.rvAlarmsHour.layoutManager = LinearLayoutManager(activity)
        adapter = AlarmsHourListAdapter(
            showTimePickerDialog = { position: Int, alarmHour: String ->
                showTimePickerDialog(position, alarmHour)
            }
        )
        binding.rvAlarmsHour.adapter = adapter

        withContext(Dispatchers.IO){
            millisList = medicinesViewModel.getMillisList(medicine.name, medicine.alarmsPerDay, medicine.treatmentID)

            // Formats the hours to 12 or 24 hours format.
            alarmHourList = editMedicinesViewModel.convertMillisToAlarmHourList(requireContext(), millisList)

            // Fill and initialize the alarm hour and minute array variables to schedule alarms
            alarmHourList?.let { alarmSettingsSharedViewModel.convertTimeListToArrays(it) }

            withContext(Dispatchers.Main){
                adapter.setAlarms(alarmHourList ?: emptyList())
            }
        }
    }


    // Opens the time picker dialog, saves the user selected hour and updates the adapter to show the new value.
    private fun showTimePickerDialog(position: Int, alarmHourString: String) {
        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val binding = LayoutTimepickerDialogBinding.inflate(layoutInflater)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(binding.root)

        val (alarmHour, alarmMinute) = editMedicinesViewModel.parseAlarmTime(alarmHourString)
        var selectedHourString = ""
        var selectedHour = 0
        var selectedMinute = 0

       binding.tpDialogTimePicker.apply{
           setIs24HourView(is24HourFormat(requireContext()))
           hour = alarmHour
           minute = alarmMinute

           setOnTimeChangedListener { _, hourOfDay, minute ->
               selectedHour = hourOfDay
               selectedMinute = minute
               selectedHourString = editMedicinesViewModel.formatTime(hourOfDay, minute, is24HourFormat(requireContext()))
           }
       }

        binding.btnOkTimePicker.setOnClickListener {
            alarmSettingsSharedViewModel.apply{
                clearAlarmArray()

                // Set the alarm hours and minutes values in the sharedviewmodel
                alarmHourList?.let { it1 -> convertTimeListToArrays(it1) }

                // Changes the alarms edited by user
                saveAlarmHour(position, selectedHour, selectedMinute)
            }

            adapter.updateAlarm(position, selectedHourString)
            dialog.dismiss()
        }

        binding.btnCancelTimePicker.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPeriodDialog(medicine: Medicine, period: String){
        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val binding = LayoutEveryXPeriodDialogBinding.inflate(layoutInflater)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(binding.root)

        binding.tvPeriodEveryDialog.text = when(period){
            "days" -> context?.getString(R.string.days)
            "weeks" -> context?.getString(R.string.weeks)
            "months" -> context?.getString(R.string.months)
            else -> ""
        }

        binding.btnOkEveryDialog.setOnClickListener {
            val interval = binding.etIntervalEveryDialog.text.toString().toInt()
            alarmSettingsSharedViewModel.setInterval(interval)

            dialog.dismiss()
        }

        binding.btnCancelEveryDialog.setOnClickListener {
            val interval = view?.findViewById<MaterialAutoCompleteTextView>(R.id.acTvMedicineFrequency)

            interval?.setText(
                when(medicine.medicineFrequency){
                    "EveryDay" -> context?.getString(R.string.every_day)
                    "EveryOtherDay" -> context?.getString(R.string.every_other_day)
                    "SpecificDaysOfWeek" -> context?.getString(R.string.specific_days_of_the_week)
                    "EveryXDays" -> context?.getString(R.string.every_x_days)
                    "EveryXWeeks" -> context?.getString(R.string.every_x_weeks)
                    "EveryXMonths" -> context?.getString(R.string.every_x_months)
                    else -> ""
                }, false
            )

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSpecificDaysDialog(){
        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val binding = LayoutSpecificDaysDialogBinding.inflate(layoutInflater)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(binding.root)
        var arrayNotEmpty = false

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
            R.color.array_list_background_dialog,
            R.color.array_list_text_color_unselected,
            R.color.selected_item_array_list_dialog,
            R.color.array_list_text_color_selected
        )
        binding.lvDayPickerDialog.adapter = arrayAdapter

        binding.lvDayPickerDialog.setOnItemClickListener { _, _, position, _ ->
            // Saves the selected days in a mutable set and return if the array is not empty
            arrayNotEmpty = arrayAdapter.checkItemSelection(position)
        }

        binding.btnOkEveryDialog.setOnClickListener {
            if(arrayNotEmpty) {
                alarmSettingsSharedViewModel.setSelectedDaysList(arrayAdapter.getSelectedDaysList())
                dialog.dismiss()
            } else {
                Toast.makeText(
                    requireContext(),
                    context?.getString(R.string.select_at_least_1_day),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.btnCancelEveryDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupAdapters(medicine: Medicine){
        val medicineForms = resources.getStringArray(R.array.medicine_forms)
        val formsArrayAdapter = NoFilterAdapter(requireContext(), R.layout.dropdown_item, medicineForms)
        binding.acTvMedicineForm.setAdapter(formsArrayAdapter)

        val medicineFrequencies = resources.getStringArray(R.array.medicine_frequency)
        val frequenciesArrayAdapter = NoFilterAdapter(requireContext(), R.layout.dropdown_item, medicineFrequencies)
        binding.acTvMedicineFrequency.setAdapter(frequenciesArrayAdapter)

        when(medicine.form){
            "pill" ->  tvStrengthVisible()
            "drop" -> tvStrengthVisible()
            "pomade" -> tvStrengthVisible()
            "injection" -> tilDoseUnitVisible(medicine, "injection")
            "liquid" -> tvStrengthVisible()
            "inhaler" -> tilDoseUnitVisible(medicine, "inhaler")
        }

        val tvstrengthtext = binding.tvStrength.text.toString()
        Log.d("tvstrength", tvstrengthtext)
    }

    private fun tvStrengthVisible(){
        binding.tvStrength.visibility = View.VISIBLE
        binding.tilDoseUnit.visibility = View.INVISIBLE
    }

    private fun tilDoseUnitVisible(medicine: Medicine, form: String) {
        binding.tvStrength.visibility = View.INVISIBLE
        binding.tilDoseUnit.visibility = View.VISIBLE

        when(form){
            "injection" -> {
                val doseUnit = resources.getStringArray(R.array.units_injection)
                val formsArrayAdapter = NoFilterAdapter(requireContext(), R.layout.dropdown_item, doseUnit)
                binding.acTvDoseUnit.setAdapter(formsArrayAdapter)
                when(medicine.unit){
                    "mL" -> binding.acTvDoseUnit.setText(getString(R.string.mL), false)
                    "syringe" -> binding.acTvDoseUnit.setText(getString(R.string.syringe), false)
                }
            }
            "inhaler" -> {
                val doseUnit = resources.getStringArray(R.array.units_inhaler)
                val formsArrayAdapter = NoFilterAdapter(requireContext(), R.layout.dropdown_item, doseUnit)
                binding.acTvDoseUnit.setAdapter(formsArrayAdapter)
                when(medicine.unit){
                    "mg" -> binding.acTvDoseUnit.setText(getString(R.string.mgs), false)
                    "puff" -> binding.acTvDoseUnit.setText(getString(R.string.puff), false)
                    "mL" -> binding.acTvDoseUnit.setText(getString(R.string.mL), false)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).findViewById<TextView>(R.id.tvSave).visibility = View.GONE
    }

    override fun onResume(){
        super.onResume()
        (activity as MainActivity).findViewById<TextView>(R.id.tvSave).visibility = View.VISIBLE
    }

    private fun setupToolbar() {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val toolbar = requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHome)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        toolbar.visibility = View.VISIBLE
    }

    private fun setupViewTheme(){
        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.colorPrimary,
            R.color.white_ice,
            R.color.fab,
            R.color.dark_gray,
            isAppearanceLightStatusBar = false,
            isAppearanceLightNavigationBar = true,
            isAppearanceLightStatusBarNightMode = false,
            isAppearanceLightNavigationBarNightMode = false
        )
    }

    private fun hideFabAndBottomNav(){
        val fabAddMedicine = requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine)
        fabAddMedicine.visibility = View.GONE

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE
    }
}