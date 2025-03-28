package com.phoenix.remedi.feature_alarms.presentation.fragments

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.FragmentHomeBinding
import com.phoenix.remedi.databinding.LayoutEndTreatmentDialogBinding
import com.phoenix.remedi.databinding.LayoutSetPillboxReminderDialogBinding
import com.phoenix.remedi.databinding.LayoutWarnAboutMedicineUsageHourBinding
import com.phoenix.remedi.feature_alarms.data.ads.Admob
import com.phoenix.remedi.feature_alarms.domain.model.Medicine
import com.phoenix.remedi.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.remedi.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.remedi.feature_alarms.presentation.AlarmReceiver
import com.phoenix.remedi.feature_alarms.presentation.HideFabScrollListener
import com.phoenix.remedi.feature_alarms.presentation.PermissionManager
import com.phoenix.remedi.feature_alarms.presentation.adapter.RvMedicinesListAdapter
import com.phoenix.remedi.feature_alarms.presentation.utils.CalendarUtils
import com.phoenix.remedi.feature_alarms.presentation.utils.LanguageConfig
import com.phoenix.remedi.feature_alarms.presentation.utils.ThemeUtils
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.AlarmSettingsSharedViewModel
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.EditMedicinesViewModel
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.HomeFragmentViewModel
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter

    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val sharedViewModel: AlarmSettingsSharedViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val editMedicinesViewModel: EditMedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val hfViewModel: HomeFragmentViewModel by viewModels()

    private var toast: Toast? = null
    private lateinit var dialog: Dialog
    private lateinit var gestureDetector: GestureDetector
    private var isPermissionDialogDisabled: Boolean = false

    private lateinit var fab: FloatingActionButton

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

        fab = requireActivity().findViewById(R.id.fabAddMedicine)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(State.STARTED) {
                // Shows a dialog requesting for app permissions. If user click on don't show again,
                // the permissionRequestPreference is updated
                launch {
                    hfViewModel.permissionRequestPreferences.collectLatest { permissionPreference ->
                        isPermissionDialogDisabled = permissionPreference

                        if(!isPermissionDialogDisabled) {
                            showRequestPermissionsDialog()
                        }
                    }
                }

                // Updates the datepicker and the pillbox reminder switch
                launch {
                    hfViewModel.pillboxReminderPreferences.collectLatest { pillboxPreference ->
                        setupDatePicker(fab, pillboxPreference)
                    }
                }

                // Reschedule alarms if app was uninstalled previously and it has a backup
                launch {
                    hfViewModel.alarmReschedulePreferences.collectLatest { alarmReschedulePreference ->
                        checkAndRescheduleAlarms(alarmReschedulePreference)
                    }
                }
            }
        }

        initRecyclerView(hfViewModel.getDate(), fab)
        setupGestureDetector()
        setupSwipeListener()
        makeFabAndRecyclerViewVisible(fab)

        fab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addMedicinesFragment)
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
//                hfViewModel.schedulePillboxReminder(hours!!, minutes!!)
                sharedPreferencesRepository.setPillboxReminderHour(hours!!, minutes!!)
                hfViewModel.schedulePillboxReminder(hours!!, minutes!!)
                hfViewModel.setPillboxPreferences(true)
            }
            dialog.dismiss()
        }

        binding.btnCancelDialogPillbox.setOnClickListener {
            dialog.dismiss()
            sharedPreferencesRepository.setPillboxReminderHour(-1, -1) // -1 == null
            hfViewModel.setPillboxPreferences(false)
            uncheckSwitch()
        }

        dialog.show()
    }

    private fun setupDatePicker(fab: FloatingActionButton, pillboxPreference: Boolean){
        val pillboxSwitch = binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox)

        //SharedPreference verification to check or uncheck the switch
        pillboxSwitch.isChecked = pillboxPreference

        //Updates the date picker and recyclerview
        binding.datePicker.onSelectionChanged = { date ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                showFab(fab)
                hfViewModel.setDate(date)
                val medicines = withContext(Dispatchers.IO){
                    medicinesViewModel.getMedicines()
                }
                adapter.setList(medicines, hfViewModel.getDate())
            }
        }

        pillboxSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showPillboxReminderDialog()
            } else {
                hfViewModel.setPillboxPreferences(false)
                sharedPreferencesRepository.setPillboxReminderHour(-1, -1) // -1 == null
                hfViewModel.cancelReminderNotifications(requireContext().applicationContext)
            }
        }
    }

    //If user has a app backup and reinstalls the app the alarms will be rescheduled at startup
    private fun checkAndRescheduleAlarms(alarmsRescheduled: Boolean) {
        if (!alarmsRescheduled) {
            val intent = Intent(requireContext(), AlarmReceiver::class.java)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = "com.phoenix.remedi.RESCHEDULEBACKUPALARMS"
            context?.sendBroadcast(intent)

            // Set the flag to true
            hfViewModel.setAlarmReschedulePreferences(true)
        }
    }

     private fun initRecyclerView(
         dateToFilter: Date,
         fab: FloatingActionButton
     ){
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
                        Admob.showInterstitial(requireActivity())
                    }
                }
            },
            markMedicinesAsSkipped = { selectedMedicine ->
                hfViewModel.markMedicinesAsSkipped(selectedMedicine)
                displayMedicinesList(hfViewModel.getDate())
                Admob.showInterstitial(requireActivity())
            },
            goToEditMedicines = { selectedMedicine ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val lastAlarm = medicinesViewModel.getLastAlarm(selectedMedicine.name, selectedMedicine.treatmentID)

                    withContext(Dispatchers.Main){
                        if (!lastAlarm.isActive) {
                            Toast.makeText(
                                requireContext(),
                                requireContext().getString(R.string.cannot_edit_finished_treatment),
                                Toast.LENGTH_LONG
                            ).show()
                            return@withContext
                        }
                        val action = HomeFragmentDirections.actionHomeFragmentToEditMedicinesFragment(lastAlarm)
                        findNavController().navigate(action)
                    }
                }

            },
            showEndTreatmentDialog = { selectedMedicine ->
                showEndTreatmentDialog(selectedMedicine)
            }
        )
        binding.rvMedicinesList.adapter = adapter
         displayMedicinesList(dateToFilter)

         val hideFabScrollListener = HideFabScrollListener(fab)
         binding.rvMedicinesList.addOnScrollListener(hideFabScrollListener)
    }

    private fun displayMedicinesList(dateToFilter: Date){
        medicinesViewModel.medicines.observe(viewLifecycleOwner) {
            adapter.setList(it, dateToFilter)
            //setCreditsVisibility()
        }
    }

    private fun uncheckSwitch(){
        binding.datePicker.findViewById<SwitchMaterial>(R.id.switchPillbox).isChecked = false
    }

    private fun showFab(fab: FloatingActionButton){
        fab.animate()
            .alpha(1f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    fab.visibility = View.VISIBLE
                }
            })
    }

    private fun showRequestPermissionsDialog(){
        if (isPermissionDialogDisabled || sharedViewModel.getPermissionDialogExhibition()){
            return
        }
        sharedViewModel.setPermissionDialogExhibition(true)

        val dialog = Dialog(this.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_overlay_permission_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val requestPermission: Button = dialog.findViewById(R.id.btnGivePermissions)
        val dismissRequest: Button = dialog.findViewById(R.id.btnDismissRequest)
        val checkboxDontShowAgain: CheckBox = dialog.findViewById(R.id.cbDontShowAgain)

        checkboxDontShowAgain.setOnCheckedChangeListener { _, isChecked ->
            hfViewModel.setPermissionRequestPreferences(isChecked)
        }

        requestPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(requireContext())){
               Toast.makeText(requireContext(), R.string.all_permissions_granted, Toast.LENGTH_LONG).show()
                checkboxDontShowAgain.isChecked = true
                dialog.dismiss()
            }
        }


        dismissRequest.setOnClickListener {
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
                        Admob.showInterstitial(requireActivity())
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
                        Admob.showInterstitial(requireActivity())
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

    private fun showWarningMedicineUsageDialog(
        medicine: Medicine
    ){
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
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.Default) {
                        medicinesViewModel.endTreatment(medicine).join()
                        sharedViewModel.cancelWork(medicine)
                    }

                    withContext(Dispatchers.Main) {
                        displayMedicinesList(hfViewModel.getDate())
                        Admob.showInterstitial(requireActivity())
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    Log.e("EndTreatmentDialog", "Error ending treatment", e)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            requireContext().getString(R.string.error_treatment_termination),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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

//    private fun setCreditsVisibility(){
//        if(binding.rvMedicinesList.adapter?.itemCount!! > 0){
//            binding.tvCredits.isVisible = true
//            return
//        }
//        binding.tvCredits.isVisible = false
//    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissionGranted: Boolean ->
        if(permissionGranted){
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine)
            fab.visibility = View.VISIBLE
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

    private fun makeFabAndRecyclerViewVisible(fab: FloatingActionButton) {
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
            binding.rvMedicinesList.visibility = View.VISIBLE
            fab.visibility = View.VISIBLE
        }

    }
    override fun onResume() {
        super.onResume()

        // Makes the toolbar invisible if user goes back during medication registration.
        val toolbar = requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHome)
        toolbar.visibility = View.GONE

        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.white_ice,
            R.color.white_ice,
            R.color.dark_gray,
            R.color.dark_gray,
            isAppearanceLightStatusBar = true,
            isAppearanceLightNavigationBar = true,
            isAppearanceLightStatusBarNightMode = false,
            isAppearanceLightNavigationBarNightMode = false
        )

        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.VISIBLE
        requireActivity().findViewById<View>(R.id.divider).visibility = View.VISIBLE
        fab.visibility = View.VISIBLE

        // Updates the medicines list.
        lifecycleScope.launch(Dispatchers.Main){
            adapter.setList(medicinesViewModel.getMedicines(), binding.datePicker.selectedDate)
        }

        // State used in EditMedicinesFragment. Everytime user navigates to EditMedicinesFragment AlarmSettingsSharedViewModel must set the treatment data.
        // The viewmodel setters are called when isInitialized is set to false. isInitialized is set to false when user goes back to HomeFragment or saves the changes.
        editMedicinesViewModel.setInitialized(false)

        // Setup app language
        val appLanguageString = sharedPreferencesRepository.getAppLanguage()

        appLanguageString?.let { language ->
            LanguageConfig.changeLanguage(language)
        }

    }

    override fun onPause() {
        super.onPause()

        //Dismiss the dialog to avoid window leakage
        if(::dialog.isInitialized && dialog.isShowing && !sharedPreferencesRepository.getPillboxPreferences()){
            dialog.dismiss()
            uncheckSwitch()
        }
    }

}



