package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentHomeBinding
import com.phoenix.pillreminder.databinding.LayoutEndTreatmentDialogBinding
import com.phoenix.pillreminder.databinding.LayoutSetPillboxReminderDialogBinding
import com.phoenix.pillreminder.databinding.LayoutWarnAboutMedicineUsageHourBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
import com.phoenix.pillreminder.feature_alarms.presentation.HideFabScrollListener
import com.phoenix.pillreminder.feature_alarms.presentation.PermissionManager
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.utils.CalendarUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.HomeFragmentViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class HomeFragment: Fragment() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository
    @Inject lateinit var alarmReceiver: AlarmReceiver

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val sharedViewModel: AlarmSettingsSharedViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private var toast: Toast? = null
    private val hfViewModel: HomeFragmentViewModel by viewModels()
    private lateinit var dialog: Dialog
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController()

        //Prevents the app from going back to an alarm registration
        requireActivity().onBackPressedDispatcher.addCallback(this){
            if(navController.currentDestination?.id == R.id.homeFragment){
                requireActivity().finish()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dontShowAgain = hfViewModel.getPermissionRequestPreferences()

        initRecyclerView(hfViewModel.getDate())
        setupGestureDetector()
        setupSwipeListener()
        checkAndRescheduleAlarms() // Reschedule alarms if app was uninstalled previously and it has a backup
        requestPermissions(dontShowAgain)
        setupDatePicker()

        binding.fabAddMedicine.setOnClickListener {
            it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
        }
    }

    private fun showPillboxReminderDialog(){
        val hourFormat = is24HourFormat(requireContext())

        dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = LayoutSetPillboxReminderDialogBinding.inflate(inflater)

        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var hours: Int? = null
        var minutes: Int? = null

        binding.tpDialogPillbox.setIs24HourView(hourFormat)

        binding.tpDialogPillbox.setOnTimeChangedListener { _, hourOfDay, minute ->
            hours = hourOfDay
            minutes = minute
        }

        binding.btnSaveDialogPillbox.setOnClickListener {
            if(hours != null && minutes != null){
                hfViewModel.schedulePillboxReminder(hours!!, minutes!!)
            }
            dialog.dismiss()
        }

        binding.btnCancelDialogPillbox.setOnClickListener {
            dialog.dismiss()
            uncheckSwitch()
        }

        dialog.show()
    }

    private fun setupDatePicker(){
        val pillboxReminder = hfViewModel.getPillboxPreferences()

        //SharedPreference verification to check or uncheck the switch
        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).isChecked = pillboxReminder

        //Updates the date picker and recyclerview
        binding.datePicker.onSelectionChanged = { date ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                hfViewModel.setDate(date)
                val medicines = withContext(Dispatchers.IO){
                    medicinesViewModel.getMedicines()
                }
                adapter.setList(medicines, hfViewModel.getDate())
            }
        }
        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hfViewModel.setPillboxPreferences(true)
                showPillboxReminderDialog()
            } else {
                hfViewModel.setPillboxPreferences(false)
                hfViewModel.cancelReminderNotifications(requireContext().applicationContext)
            }
        }
    }

    //If user has a app backup and reinstalls the app the alarms will be rescheduled at startup
    private fun checkAndRescheduleAlarms(){
        val alarmsRescheduled = sharedPreferencesRepository.getAlarmReschedulePreferences()
        Log.d("debug", "alarms rescheduled: $alarmsRescheduled")
        if (!alarmsRescheduled) {
            val intent = Intent(requireContext(), AlarmReceiver::class.java)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = "com.phoenix.pillreminder.RESCHEDULEBACKUPALARMS"
            context?.sendBroadcast(intent)

            // Set the flag to true
            sharedPreferencesRepository.setAlarmReschedulePreferences(true)
        }
    }

    private fun requestPermissions(dontShowAgain: Boolean){
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
            binding.rvMedicinesList.visibility = View.VISIBLE
            binding.fabAddMedicine.visibility = View.VISIBLE
        }
        if(!Settings.canDrawOverlays(requireContext()) && !dontShowAgain){
            showOverlayAndNotificationPermissionDialog()
        }
    }

     private fun initRecyclerView(dateToFilter: Date){
        binding.rvMedicinesList.layoutManager = LinearLayoutManager(activity)
        adapter = RvMedicinesListAdapter(
            showDeleteAlarmDialog = { selectedMedicine: Medicine ->
                showDeleteAlarmDialog(selectedMedicine)
            },
            showDeleteAllAlarmsDialog = { selectedMedicine: Medicine ->
                showDeleteAllAlarmsDialog(selectedMedicine)
            },
            markMedicineUsage = { selectedMedicine: Medicine ->
                isNextToAnotherDoseHour(selectedMedicine) { result ->
                    if(result){
                        showWarningMedicineUsageDialog(selectedMedicine)
                    }
                    else{
                        hfViewModel.markMedicineUsage(selectedMedicine)
                        displayMedicinesList(hfViewModel.getDate())
                    }
                }
            },
            markMedicinesAsSkipped = { selectedMedicine ->
                hfViewModel.markMedicinesAsSkipped(selectedMedicine)
                displayMedicinesList(hfViewModel.getDate())
            },
            goToEditMedicines = { selectedMedicine ->
                val action = HomeFragmentDirections.actionHomeFragmentToEditMedicinesFragment(selectedMedicine)
                findNavController().navigate(action)
            },
            showEndTreatmentDialog = { selectedMedicine ->
                showEndTreatmentDialog(selectedMedicine)
            }
        )
        binding.rvMedicinesList.adapter = adapter
         displayMedicinesList(dateToFilter)

         val hideFabScrollListener = HideFabScrollListener(binding.fabAddMedicine)
         binding.rvMedicinesList.addOnScrollListener(hideFabScrollListener)
    }

    private fun displayMedicinesList(dateToFilter: Date){
        medicinesViewModel.medicines.observe(viewLifecycleOwner) {
            adapter.setList(it, dateToFilter)
            setCreditsVisibility()
        }
    }

    private fun uncheckSwitch(){
        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).isChecked = false
    }

    private fun showOverlayAndNotificationPermissionDialog(){
        val dontShowAgain = hfViewModel.getPermissionRequestPreferences()

        if (dontShowAgain){
            return
        }

        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_overlay_permission_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val requestPermission: Button = dialog.findViewById(R.id.btnGivePermissions)
        val dismissRequest: Button = dialog.findViewById(R.id.btnDismissRequest)
        val checkboxDontShowAgain: CheckBox = dialog.findViewById(R.id.cbDontShowAgain)

        requestPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        dismissRequest.setOnClickListener {
            if(checkboxDontShowAgain.isChecked){
                hfViewModel.setPermissionRequestPreferences(true)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteAlarmDialog(medicine: Medicine){
        dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_delete_alarm_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMedicine: TextView = dialog.findViewById(R.id.tvMedicineAndHour)
        val btnDelete: Button = dialog.findViewById(R.id.btnDelete)
        val btnCancel: Button = dialog.findViewById(R.id.btnCancel)

        tvMedicine.text = context?.getString(R.string.tv_alarm_and_hour, medicine.name, showTvAlarm(medicine.alarmHour, medicine.alarmMinute))

        btnDelete.setOnClickListener {
            medicinesViewModel.apply{
                viewLifecycleOwner.lifecycleScope.launch{
                    //Cancel alarms if needed
                    hfViewModel.cancelAlarm(medicine, false)

                    //Work cancel
                    sharedViewModel.cancelWork(medicine)

                    //Database medicine deletion
                    deleteMedicines(medicine)

                    withContext(Dispatchers.Main){
                        displayMedicinesList(hfViewModel.getDate())
                        dialog.dismiss()
                        showToastAlarmDeleted()
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteAllAlarmsDialog(medicine: Medicine){
        dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_delete_all_alarms_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMedicine: TextView = dialog.findViewById(R.id.tvMedicineAndHour)
        val btnDelete: Button = dialog.findViewById(R.id.btnDeleteAll)
        val btnCancel: Button = dialog.findViewById(R.id.btnCancel)

        tvMedicine.text = context?.getString(R.string.tv_medicine, medicine.name)

        btnDelete.setOnClickListener {
            hfViewModel.apply{
                viewLifecycleOwner.lifecycleScope.launch{
                    cancelAlarm(medicine, true)
                    sharedViewModel.cancelWork(medicine)
                    deleteAllMedicinesWithSameName(medicine.name).join()

                    withContext(Dispatchers.Main){
                        displayMedicinesList(getDate())
                        dialog.dismiss()
                        showToastAlarmDeleted()
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showWarningMedicineUsageDialog(medicine: Medicine){
        dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = LayoutWarnAboutMedicineUsageHourBinding.inflate(inflater)

        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btnMarkMedicineUsage.setOnClickListener {
            hfViewModel.markMedicineUsage(medicine)
            displayMedicinesList(hfViewModel.getDate())
            dialog.dismiss()
        }

        binding.btnSkipDose.setOnClickListener {
            hfViewModel.markMedicinesAsSkipped(medicine)
            displayMedicinesList(hfViewModel.getDate())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEndTreatmentDialog(medicine: Medicine){
        dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = LayoutEndTreatmentDialogBinding.inflate(inflater)

        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvYouSure.text = context?.getString(R.string.are_you_sure_end_treatment, medicine.name)

        binding.btnEndTreatment.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                medicinesViewModel.endTreatment(medicine).join()
                displayMedicinesList(hfViewModel.getDate())
                dialog.dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isNextToAnotherDoseHour(selectedMedicine: Medicine, callback: (Boolean) -> Unit){
        hfViewModel.isNextToAnotherDoseHour(selectedMedicine, callback)
    }

    private fun showToastAlarmDeleted(){
        //Checks if a Toast is currently being displayed
        if (toast != null){
            toast?.cancel()
        }
        toast = Toast.makeText(context,
            getString(R.string.alarm_successfully_deleted), Toast.LENGTH_LONG)
        toast?.show()
    }

    private fun showTvAlarm(alarmHour: Int, alarmMinute: Int): String{
        val context = requireContext()
        when {
            is24HourFormat(context) -> {
                return CalendarUtils.formatHour(alarmHour, alarmMinute, "HH:mm")
            }
            !is24HourFormat(context) -> {
                return CalendarUtils.formatHour(alarmHour, alarmMinute, "hh:mm a")
            }
        }
        return ""
    }

    private fun setCreditsVisibility(){
        if(binding.rvMedicinesList.adapter?.itemCount!! > 0){
            binding.tvCredits.isVisible = true
            return
        }
        binding.tvCredits.isVisible = false
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissionGranted: Boolean ->
        if(permissionGranted){
            binding.fabAddMedicine.visibility = View.VISIBLE
            binding.rvMedicinesList.visibility = View.VISIBLE
        }
        requestOverlayPermission(requireContext())
    }

    private fun requestOverlayPermission(context: Context){
        PermissionManager.apply{
            if(!canDrawOverlays(context)){
                requestOverlayPermissionLauncher.launch(getOverlayPermissionIntent(context))
            }
        }
    }

    private val requestOverlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)
                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right
                        updateDateAndMedicines(-1)
                    } else {
                        // Swipe left
                        updateDateAndMedicines(1)
                    }
                    return true
                }
                return false
            }
        })
    }

    private fun setupSwipeListener() {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.swipeRefreshLayout.isEnabled = false
        binding.rvMedicinesList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun updateDateAndMedicines(days: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newDate = Calendar.getInstance().apply {
                time = hfViewModel.getDate()
                add(Calendar.DAY_OF_MONTH, days)
            }.time

            hfViewModel.setDate(newDate)
            binding.datePicker.setSelection(newDate)

            val medicines = withContext(Dispatchers.IO) {
                medicinesViewModel.getMedicines()
            }

            // Apply swipe animation
            val animator = if (days > 0) {
                ObjectAnimator.ofFloat(binding.rvMedicinesList, View.TRANSLATION_X, binding.rvMedicinesList.width.toFloat(), 0f)
            } else {
                ObjectAnimator.ofFloat(binding.rvMedicinesList, View.TRANSLATION_X, -binding.rvMedicinesList.width.toFloat(), 0f)
            }

            animator.duration = 200
            animator.start()

            adapter.setList(medicines, newDate)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main){
            val currentDate = Date(System.currentTimeMillis())
            adapter.setList(medicinesViewModel.getMedicines(), currentDate)
        }
    }

    override fun onPause() {
        super.onPause()

        //Dismiss the dialog to avoid window leakage
        if(::dialog.isInitialized && dialog.isShowing){
            dialog.dismiss()
        }

        val switchPillbox = binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox)

        //If user minimizes or closes the app with pillbox reminder dialog opened, the switch is unchecked
        if(switchPillbox.isChecked && !hfViewModel.isWorkerActive()){
            switchPillbox.isChecked = false
        }
    }

}



