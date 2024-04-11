package com.phoenix.pillreminder.fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.MainActivity
import com.phoenix.pillreminder.adapter.RvMedicinesListAdapter
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AlarmScheduler
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.databinding.FragmentHomeBinding
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.model.HomeFragmentViewModel
import com.phoenix.pillreminder.model.MedicinesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter
    private lateinit var medicinesViewModel: MedicinesViewModel
    private var toast: Toast? = null
    private var wasOverlayPermissionDialogShown: Boolean = false
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

        medicinesViewModel = ViewModelProvider(requireActivity(), (requireActivity() as MainActivity).factory)[MedicinesViewModel::class.java]

        //Toolbar setup
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbarHome.setupWithNavController(navController, appBarConfiguration)
        binding.toolbarHome.title = "Pill Reminder"
        binding.toolbarHome.setTitleTextColor(Color.WHITE)

        initRecyclerView(hfViewModel.getDate())

        binding.datePicker.onSelectionChanged = { date ->
            CoroutineScope(Dispatchers.Main).launch{
                hfViewModel.setDate(date)
                val medicines = withContext(Dispatchers.IO){
                    medicinesViewModel.getMedicines()
                }
                adapter.setList(medicines, hfViewModel.getDate())
            }
        }

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
            binding.rvMedicinesList.visibility = View.VISIBLE
            binding.fabAddMedicine.visibility = View.VISIBLE
        }

        if(!Settings.canDrawOverlays(requireContext()) && !wasOverlayPermissionDialogShown){
            showOverlayPermissionDialog()
            wasOverlayPermissionDialogShown = true
        }

        binding.fabAddMedicine.setOnClickListener {
            it.findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
        }

    }

     private fun initRecyclerView(dateToFilter: Date){
        binding.rvMedicinesList.layoutManager = LinearLayoutManager(activity)
        adapter = RvMedicinesListAdapter{
            selectedMedicine: Medicine -> showDeleteAlarmDialog(selectedMedicine)
        }
        binding.rvMedicinesList.adapter = adapter

        displayMedicinesList(dateToFilter)
    }

    private fun displayMedicinesList(dateToFilter: Date){
        medicinesViewModel.medicines.observe(viewLifecycleOwner) {
            adapter.setList(it, dateToFilter)
            setCreditsVisibility()
        }
    }

    private fun showOverlayPermissionDialog(){
        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_overlay_permission_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val requestPermission: Button = dialog.findViewById(R.id.btnGivePermissions)
        val dismissRequest: Button = dialog.findViewById(R.id.btnDismissRequest)

        requestPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        dismissRequest.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun showDeleteAlarmDialog(medicine: Medicine){
        dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_delete_alarm_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMedicine: TextView = dialog.findViewById(R.id.tvMedicineAndHour)
        val btnDelete: Button = dialog.findViewById(R.id.btnDelete)
        val btnDeleteAll: Button = dialog.findViewById(R.id.btnDeleteAll)
        val btnCancel: Button = dialog.findViewById(R.id.btnCancel)

        tvMedicine.text = context?.getString(R.string.tv_alarm_and_hour, medicine.name, showTvAlarm(medicine.alarmHour, medicine.alarmMinute))

        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(requireActivity().applicationContext)

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        btnDelete.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch{
                //Checks if the alarm was already triggered. If so, there is no need to cancel the broadcast.
                if(medicine.alarmInMillis > System.currentTimeMillis()){
                    alarmScheduler.cancelAlarm(alarmItem, false)
                }
                Log.i("alarmdata", "medicine name ${medicine.name} and currentmillis ${System.currentTimeMillis()}")
                withContext(Dispatchers.IO){
                    val hasNextAlarm = medicinesViewModel.hasNextAlarmData(medicine.name, System.currentTimeMillis())
                    Log.i("alarmdata", hasNextAlarm.toString())

                    if(!hasNextAlarm){
                        val workRequestID = UUID.fromString(medicinesViewModel.getWorkerID(medicine.name))
                        WorkManager.getInstance(requireContext().applicationContext).cancelWorkById(workRequestID)
                    }
                }
                medicinesViewModel.deleteMedicines(medicine)
                displayMedicinesList(hfViewModel.getDate())
                dialog.dismiss()
                showToastAlarmDeleted()
            }
        }

        btnDeleteAll.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch{
                alarmScheduler.cancelAlarm(alarmItem, true)

                withContext(Dispatchers.IO){
                    val workRequestID = UUID.fromString(medicinesViewModel.getWorkerID(medicine.name))
                    val workManager = WorkManager.getInstance(requireContext().applicationContext).cancelWorkById(workRequestID)
                    val workInfo = workManager.state
                    Log.i("WorkManager", "${workInfo.value}")

                    val alarmsToDelete = medicinesViewModel.getAllMedicinesWithSameName(medicine.name)
                    medicinesViewModel.deleteAllSelectedMedicines(alarmsToDelete)
                }

                displayMedicinesList(hfViewModel.getDate())
                dialog.dismiss()
                showToastAlarmDeleted()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
            DateFormat.is24HourFormat(context) -> {
                return formatHour(alarmHour, alarmMinute, "HH:mm")
            }
            !DateFormat.is24HourFormat(context) -> {
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
        if(!Settings.canDrawOverlays(requireActivity().applicationContext)){
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
}



