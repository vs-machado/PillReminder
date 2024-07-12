package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.FragmentHomeBinding
import com.phoenix.pillreminder.databinding.LayoutSetPillboxReminderDialogBinding
import com.phoenix.pillreminder.databinding.LayoutWarnAboutMedicineUsageHourBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.HideFabScrollListener
import com.phoenix.pillreminder.feature_alarms.presentation.PermissionManager
import com.phoenix.pillreminder.feature_alarms.presentation.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.feature_alarms.presentation.utils.CalendarUtils
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.HomeFragmentViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment: Fragment() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var alarmScheduler: AndroidAlarmScheduler

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

        //SharedPreference verification to check or uncheck the switch
        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).isChecked = pillboxReminder

        requestPermissions(dontShowAgain)

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


        binding.fabAddMedicine.setOnClickListener {
            it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
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
                        hfViewModel.markMedicineUsage(selectedMedicine)
                        displayMedicinesList(hfViewModel.getDate())
                    }
                }
            },
            markMedicinesAsSkipped = {selectedMedicine ->
                hfViewModel.markMedicinesAsSkipped(selectedMedicine)
                displayMedicinesList(hfViewModel.getDate())
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

                    withContext(Dispatchers.IO){
                        //Work cancel
                        hfViewModel.cancelWork(medicine, getWorkerID(medicine.name))
                    }

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

                    withContext(Dispatchers.IO){
                        cancelWork(medicine, medicinesViewModel.getWorkerID(medicine.name))
                    }

                    deleteAllMedicinesWithSameName(medicine.name)

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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main){
            adapter.setList(medicinesViewModel.getMedicines(), hfViewModel.getDate())
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



