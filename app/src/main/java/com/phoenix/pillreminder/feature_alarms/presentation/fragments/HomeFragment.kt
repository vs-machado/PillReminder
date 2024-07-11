package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.view.LayoutInflater
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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentHomeBinding
import com.phoenix.pillreminder.databinding.LayoutSetPillboxReminderDialogBinding
import com.phoenix.pillreminder.databinding.LayoutWarnAboutMedicineUsageHourBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.HideFabScrollListener
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.HomeFragmentViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment: Fragment() {

    @Inject
    lateinit var repository: MedicineRepository

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter
    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private var toast: Toast? = null
    private val hfViewModel: HomeFragmentViewModel by viewModels()
    private lateinit var dialog: Dialog

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

        val pillboxReminder = hfViewModel.getPillboxPreferences()
        val dontShowAgain = hfViewModel.getPermissionRequestPreferences()

        initRecyclerView(hfViewModel.getDate())
        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).isChecked = pillboxReminder
        requestPermissions(dontShowAgain)

        binding.datePicker.onSelectionChanged = { date ->
            CoroutineScope(Dispatchers.Main).launch{
                hfViewModel.setDate(date)
                val medicines = withContext(Dispatchers.IO){
                    medicinesViewModel.getMedicines()
                }
                adapter.setList(medicines, hfViewModel.getDate())
            }
        }


        binding.fabAddMedicine.setOnClickListener {
            it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
        }


        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).setOnCheckedChangeListener { _, isChecked ->
            val alarmScheduler = AndroidAlarmScheduler(repository, requireContext())
            val hourFormat = is24HourFormat(requireContext())

            if (isChecked) {
                hfViewModel.setPillboxPreferences(true)

                dialog = Dialog(this.requireContext())
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(false)

                val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val binding = LayoutSetPillboxReminderDialogBinding.inflate(inflater)

                dialog.setContentView(binding.root)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                var pillboxReminderHour: Int? = null
                var pillboxReminderMinute: Int? = null

                binding.tpDialogPillbox.setIs24HourView(hourFormat)

                binding.tpDialogPillbox.setOnTimeChangedListener { _, hourOfDay, minute ->
                    pillboxReminderHour = hourOfDay
                    pillboxReminderMinute = minute
                }

                binding.btnSaveDialogPillbox.setOnClickListener {
                    if(pillboxReminderHour != null && pillboxReminderMinute != null){
                        Log.d("Alarm", "homefragment")
                        alarmScheduler.schedulePillboxReminder(pillboxReminderHour!!,
                            pillboxReminderMinute!!
                        )
                    }
                    dialog.dismiss()
                }

                binding.btnCancelDialogPillbox.setOnClickListener {
                    dialog.dismiss()
                    uncheckSwitch()
                }

                dialog.show()
            } else {
                hfViewModel.setPillboxPreferences(false)
                hfViewModel.cancelReminderNotifications(requireContext().applicationContext)
            }
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
            showDeleteAlarmDialog = {selectedMedicine: Medicine ->
                showDeleteAlarmDialog(selectedMedicine)
            },
            showDeleteAllAlarmsDialog = {selectedMedicine: Medicine ->
                showDeleteAllAlarmsDialog(selectedMedicine)
            },
            markMedicineUsage = {selectedMedicine: Medicine ->
                isNextToAnotherDoseHour(selectedMedicine) { result ->
                    if(result){
                        showWarningMedicineUsageDialog(selectedMedicine)
                    }
                    else{
                        markMedicineUsage(selectedMedicine)
                    }
                }
            },
            markMedicinesAsSkipped = {selectedMedicine ->
                markMedicinesAsSkipped(selectedMedicine)
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

        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(repository, requireContext())

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        btnDelete.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch{
                //Checks if the alarm was already triggered. If so, there is no need to cancel the broadcast.
                if(medicine.alarmInMillis > System.currentTimeMillis()){
                    alarmScheduler.cancelAlarm(alarmItem, false)
                }

                //Work cancel
                val hasNextAlarm = medicinesViewModel.hasNextAlarmData(medicine.name, System.currentTimeMillis())

                if(!hasNextAlarm){
                    hfViewModel.cancelWork(medicinesViewModel.getWorkerID(medicine.name), requireContext())
                }

                /*
                val firstMedicineOfTheDay = medicinesViewModel.getFirstMedicineOfTheDay(
                    hfViewModel.getUserMidnightMillis()
                )
                val firstMedicineOfNextDay = medicinesViewModel.getFirstMedicineOfNextDay(
                    hfViewModel.getNextDayMidnightMillis(System.currentTimeMillis())
                )

                if(userSetPillboxReminders && (medicine == firstMedicineOfNextDay
                            || medicine == firstMedicineOfTheDay && medicine.alarmInMillis > System.currentTimeMillis() )){

                    hfViewModel.cancelReminderNotifications(requireContext())
                    medicinesViewModel.getFirstMedicineOfTheDay()
                    alarmScheduler.schedulePillboxReminder()

                }
                else if (userSetPillboxReminders && medicine != firstMedicineOfNextDay){

                }
                else {
                    hfViewModel.cancelReminderNotifications(requireContext())
                }*/

                medicinesViewModel.deleteMedicines(medicine)

                withContext(Dispatchers.Main){
                    displayMedicinesList(hfViewModel.getDate())
                    dialog.dismiss()
                    showToastAlarmDeleted()
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

        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(repository, requireContext())

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        btnDelete.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch{
                alarmScheduler.cancelAlarm(alarmItem, true)

                val workRequestID = medicinesViewModel.getWorkerID(medicine.name)

                if(workRequestID != "noID"){
                    val workRequestUUID = UUID.fromString(workRequestID)
                    WorkManager.getInstance(requireContext().applicationContext).cancelWorkById(workRequestUUID)
                }

                val alarmsToDelete = medicinesViewModel.getAllMedicinesWithSameName(medicine.name)
                medicinesViewModel.deleteAllSelectedMedicines(alarmsToDelete)

                withContext(Dispatchers.Main){
                    displayMedicinesList(hfViewModel.getDate())
                    dialog.dismiss()
                    showToastAlarmDeleted()
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
            markMedicineUsage(medicine)
            dialog.dismiss()
        }

        binding.btnSkipDose.setOnClickListener {
            markMedicinesAsSkipped(medicine)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isNextToAnotherDoseHour(selectedMedicine: Medicine, callback: (Boolean) -> Unit){
        val usageHour = selectedMedicine.alarmInMillis

        CoroutineScope(Dispatchers.IO).launch{
            val nextAlarmHour = medicinesViewModel.getNextAlarmData(selectedMedicine.name, selectedMedicine.alarmInMillis)?.alarmInMillis

            withContext(Dispatchers.Main){
                val intervalBetweenAlarms = nextAlarmHour?.minus(usageHour)

                if(intervalBetweenAlarms != null){
                    val closeToNextAlarm = (System.currentTimeMillis() - usageHour) > ((2.0/3.0) * intervalBetweenAlarms)
                    val pastTheNextAlarmHour = System.currentTimeMillis() > nextAlarmHour

                    //If user is next to the next alarm hour a warning will be displayed asking if he really wants to use the medicine
                    if(closeToNextAlarm || pastTheNextAlarmHour){
                        callback(true)
                    } else{
                        callback(false)
                    }
                }
            }
        }
    }

    private fun markMedicineUsage(medicine: Medicine){
        medicine.medicineWasTaken = true
        medicinesViewModel.updateMedicines(medicine)
        displayMedicinesList(hfViewModel.getDate())
    }

    private fun markMedicinesAsSkipped(medicine: Medicine){
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(repository, requireContext())
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        medicine.wasSkipped = true

        CoroutineScope(Dispatchers.IO).launch{
            medicinesViewModel.updateMedicines(medicine)

            withContext(Dispatchers.Main){
                /*Checks if the alarm was already triggered. If so, there is no need to cancel the broadcast.
                cancelAlarm() will cancel the alarm and check if there is another alarm to be scheduled*/
                if(medicine.alarmInMillis > System.currentTimeMillis()){
                    alarmScheduler.cancelAlarm(alarmItem, false)
                }
            }

            val hasNextAlarm = medicinesViewModel.hasNextAlarmData(medicine.name, System.currentTimeMillis())

            withContext(Dispatchers.Main){
                if(!hasNextAlarm){
                    val workRequestID = UUID.fromString(medicinesViewModel.getWorkerID(medicine.name))
                    WorkManager.getInstance(requireContext().applicationContext).cancelWorkById(workRequestID)
                }

                displayMedicinesList(hfViewModel.getDate())
            }
        }
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
                return formatHour(alarmHour, alarmMinute, "HH:mm")
            }
            !is24HourFormat(context) -> {
                return formatHour(alarmHour, alarmMinute, "hh:mm a")
            }
        }
        return ""
    }

    private fun formatHour(hour: Int, minute: Int, pattern: String): String{
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun setCreditsVisibility(){
        if(binding.rvMedicinesList.adapter?.itemCount!! > 0){
            binding.tvCredits.isVisible = true
            return
        }
        binding.tvCredits.isVisible = false
    }

    private fun requestOverlayPermission(){
        if(!Settings.canDrawOverlays(requireContext())){
            val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply{
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("package:${requireContext().packageName}")
            }
            requestOverlayPermissionLauncher.launch(overlayIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main){
            adapter.setList(medicinesViewModel.getMedicines(), hfViewModel.getDate())
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissionGranted: Boolean ->
        if(permissionGranted){
            binding.fabAddMedicine.visibility = View.VISIBLE
            binding.rvMedicinesList.visibility = View.VISIBLE
        }
        requestOverlayPermission()
    }

    private val requestOverlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}

    override fun onPause() {
        super.onPause()

        //Dismiss the dialog to avoid window leakage
        if(::dialog.isInitialized && dialog.isShowing){
            dialog.dismiss()
        }

        val switchPillbox = binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox)

        Log.d("Worker", "${isWorkerActive()}")

        //If user minimizes or closes the app with pillboxreminder dialog opened, the switch is unchecked
        if(switchPillbox.isChecked && !isWorkerActive()){
            switchPillbox.isChecked = false
        }
    }

    private fun isWorkerActive(): Boolean{
        val workManager = WorkManager.getInstance(requireContext())
        val workInfoList = workManager.getWorkInfosForUniqueWork("PillboxReminder").get()

        for(workInfo in workInfoList){
            if(workInfo.state == WorkInfo.State.ENQUEUED) {
                return true
            }
        }
        return false
    }

}



