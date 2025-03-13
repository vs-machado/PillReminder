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

/**
 * Main fragment for displaying and managing medication alarms.
 *
 * This fragment handles:
 * - Displaying a list of medication alarms
 * - Adding new medication alarms via FAB
 * - Managing daily pillbox reminders
 * - Handling user permissions (notifications and overlay)
 * - Date navigation for viewing alarms on different days
 * - Backup alarm rescheduling after app reinstallation
 *
 * Key features:
 * - Swipe gestures for date navigation
 * - Permission request dialogs
 * - Pillbox reminder configuration
 * - Medicine usage tracking
 * - Treatment management (end/edit/delete)
 */
@AndroidEntryPoint
class HomeFragment: Fragment() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: RvMedicinesListAdapter

    private val medicinesViewModel: MedicinesViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val sharedViewModel: AlarmSettingsSharedViewModel by hiltNavGraphViewModels(R.id.nav_graph)
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

    /**
     * Displays a dialog allowing users to set a daily reminder time for pillbox refills.
     *
     * When saved:
     * - Stores the time in SharedPreferences
     * - Schedules the daily reminder
     * - Updates the pillbox reminder preferences
     */
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

    /**
     * Configures the date picker calendar and pillbox reminder switch.
     *
     * This function:
     * - Initializes the pillbox reminder switch state based on saved preferences
     * - Sets up date selection handling to update medicine list
     * - Manages pillbox reminder enabling/disabling
     *
     * Date selection triggers:
     * - FAB visibility update
     * - Date update in view model
     * - Medicine list refresh for selected date
     *
     * @param fab The FloatingActionButton to show/hide during date changes
     * @param pillboxPreference The current state of pillbox reminder preference
     */
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
                sharedPreferencesRepository.setPillboxReminderHour(-1, -1) // -1 == null, SharedPreferences only supports primitive types
                hfViewModel.cancelReminderNotifications(requireContext().applicationContext)
            }
        }
    }

    /**
     * Reschedules alarms from backup data when the app is reinstalled.
     *
     * This function broadcasts an intent to reschedule all alarms if they haven't
     * been rescheduled yet. After rescheduling, it sets a flag to prevent
     * duplicate rescheduling.
     *
     * @param alarmsRescheduled Flag indicating if alarms have already been rescheduled
     */
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

    /**
     * Shows a dialog requesting notification and overlay permissions from the user.
     * *
     * The dialog won't be shown if:
     * - User has previously selected "don't show again"
     * - The dialog has already been shown in the current session
     */
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
    
    
    /**
     * Shows a dialog to confirm deletion of a single medicine alarm
     * 
     * @param medicine The medicine object containing alarm details to be deleted
     *
     * This dialog allows users to:
     * - View the medicine name and scheduled alarm time
     * - Delete the specific alarm instance
     * - Cancel the deletion operation
     *
     * When deleted:
     * 1. Cancels the scheduled alarm
     * 2. Cancels associated background work (if exists)
     * 3. Removes the medicine from database
     * 4. Updates the UI and shows confirmation toast
     */
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

    /**
     * Shows a confirmation dialog for deleting all alarms associated with a medicine.
     *
     * This dialog allows users to:
     * - View the medicine name to be deleted
     * - Confirm deletion of all associated alarms
     * - Cancel the deletion operation
     *
     * When deletion is confirmed:
     * 1. Cancels all scheduled alarms for the medicine
     * 2. Cancels associated background work
     * 3. Deletes all medicine records with the same name
     * 4. Updates the UI and displays an ad
     * 5. Shows confirmation toast
     *
     * @param medicine The medicine object containing details of alarms to be deleted
     */
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

    /**
     * Shows a warning dialog when attempting to mark medicine usage near another dose time.
     *
     * This dialog appears when a user tries to mark a medicine as taken at a time
     * that's close to another scheduled dose. It provides two options:
     * - Mark the medicine as taken anyway
     * - Skip the dose instead
     *
     * Both actions will:
     * - Update the medicine status in the database
     * - Refresh the medicines list display
     * - Dismiss the dialog
     *
     * @param medicine The medicine being marked as taken or skipped
     */
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

    /**
     * Shows a confirmation dialog for ending a medicine treatment.
     *
     * This dialog:
     * - Displays a confirmation message with the medicine name
     * - Provides options to end treatment or cancel
     * - Handles errors during treatment termination
     *
     * When treatment is ended:
     * 1. Updates the treatment status in the database
     * 2. Cancels any associated background work
     * 3. Refreshes the medicines list
     * 4. Shows an interstitial ad
     *
     * Error handling:
     * - Logs errors to LogCat with "EndTreatmentDialog" tag
     * - Shows error toast message to user
     *
     * @param medicine The medicine treatment to be ended
     */
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

                        dialog.dismiss()
                    }
                }
            }
        }


        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Checks if the current medicine is next to another dose hour to prevent accidental overlaps or scheduling issues.
     * When it's next to another dose, returns a boolean callback used to show a warning.
     *
     * @param selectedMedicine the medicine object to check for adjacent dose hours
     * @param callback a boolean callback to indicate whether the medicine is next to another dose hour
     */
    private fun isNextToAnotherDoseHour(selectedMedicine: Medicine, callback: (Boolean) -> Unit){
        hfViewModel.isNextToAnotherDoseHour(selectedMedicine, callback)
    }

    /**
     * Shows a toast message confirming alarm deletion.
     *
     * This function:
     * 1. Cancels any existing toast to prevent stacking
     * 2. Creates and shows a new toast with the deletion confirmation message
     *
     * The toast reference is stored in a class-level variable to allow
     * proper cancellation of previous toasts.
     */
    private fun showToastAlarmDeleted(){
        //Checks if a Toast is currently being displayed
        if (toast != null){
            toast?.cancel()
        }
        toast = Toast.makeText(context,
            getString(R.string.alarm_successfully_deleted), Toast.LENGTH_LONG)
        toast?.show()
    }

    /**
     * Formats the alarm hour textview based on system's time format (12-hour or 24-hour)
     *
     * @param alarmHour the hour of the alarm
     * @param alarmMinute the minute of the alarm
     *
     * @return a formatted string representing the alarm time
     */
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

    // Used to request post notifications permissions and then ask for overlay permission.
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

    /**
     * Configures gesture detection for swipe navigation between dates.
     *
     * Sets up a [GestureDetector] to handle horizontal swipe gestures that:
     * - Detects left/right swipes based on defined thresholds
     * - Ignores vertical swipes
     * - Updates the date and medicine list accordingly
     *
     * Swipe behavior:
     * - Right swipe -> Previous day (date - 1)
     * - Left swipe -> Next day (date + 1)
     *
     * Note: The gesture detector is attached to the RecyclerView through [setupSwipeListener]
     */
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

    /**
     * Configures touch event handling for the medicine list's swipe functionality.
     *
     * This function:
     * - Disables the SwipeRefreshLayout's default refresh behavior
     * - Sets up touch event forwarding to the [gestureDetector]
     * - Enables horizontal swipe navigation while maintaining vertical scrolling
     *
     * Implementation details:
     * - Disables pull-to-refresh to prevent conflicts with swipe gestures
     * - Forwards all touch events to the gesture detector for processing
     * - Returns false to allow the RecyclerView to handle other touch events
     *   (like scrolling) normally
     *
     * Note: Works in conjunction with [setupGestureDetector] to handle date navigation
     */
    private fun setupSwipeListener() {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.swipeRefreshLayout.isEnabled = false
        binding.rvMedicinesList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    /**
     * Updates the displayed date and medicine list with an animated transition.
     *
     * This coroutine-based function:
     * 1. Calculates and sets the new date based on day offset
     * 2. Updates the date picker UI
     * 3. Fetches updated medicine list on IO dispatcher
     * 4. Applies a slide animation based on navigation direction
     * 5. Updates the RecyclerView with new data
     *
     * @param days Integer offset for date navigation:
     *             Positive values make list swipe to left
     *             Negative values make list swipe to right
     */
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

    /**
     *  Change views visibility to View.VISIBLE when POST_NOTIFICATIONS permission is granted
     *
     *  @param fab The MainActivity FloatingActionButton that will be made visible
     */
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

        // Sets theme colors
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
        sharedViewModel.setInitialized(false)

        // Setup app language
        val appLanguageString = sharedPreferencesRepository.getAppLanguage()

        appLanguageString?.let { language ->
            LanguageConfig.changeLanguage(language)
        }

    }

    override fun onPause() {
        super.onPause()

        // Dismiss the pillbox reminder dialog to avoid window leakage
        if(::dialog.isInitialized && dialog.isShowing && !sharedPreferencesRepository.getPillboxPreferences()){
            dialog.dismiss()
            uncheckSwitch()
        }
    }

}



