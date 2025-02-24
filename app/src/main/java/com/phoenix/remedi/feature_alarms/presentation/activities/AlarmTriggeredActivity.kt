package com.phoenix.remedi.feature_alarms.presentation.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.ActivityAlarmTriggeredBinding
import com.phoenix.remedi.feature_alarms.data.ads.Admob
import com.phoenix.remedi.feature_alarms.data.ads.Admob.loadAd
import com.phoenix.remedi.feature_alarms.domain.model.AlarmItem
import com.phoenix.remedi.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.remedi.feature_alarms.presentation.AlarmScheduler
import com.phoenix.remedi.feature_alarms.presentation.AlarmService
import com.phoenix.remedi.feature_alarms.presentation.utils.DateUtil.localDateTimeToMillis
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.AlarmTriggeredViewModel
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 *  Activity responsible for showing the medicine details when an alarm is triggered,
 *  allowing user to mark the medicine usage, snooze alarms or dismiss the notification.
 *  AlarmTriggeredActivity is only shown if user grants notification permission and enables
 *  app overlay permission in phone settings.
 *
 *  The activity shows the following medicine properties:
 *  Medicine name, alarm hour, medicine type image and dosage.
 */
@AndroidEntryPoint
class AlarmTriggeredActivity: AppCompatActivity() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private lateinit var medicinesViewModel: MedicinesViewModel
    private lateinit var binding: ActivityAlarmTriggeredBinding
    private val viewModel: AlarmTriggeredViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAlarmTriggeredBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(binding.root)

        setupNotificationAndStatusBar()

        val initializeAds: () -> Unit = {
            CoroutineScope(Dispatchers.IO).launch {
                // Initialize the Google Mobile Ads SDK on a background thread.
                MobileAds.initialize(this@AlarmTriggeredActivity) {}
                runOnUiThread {
                    // Load an ad on the main thread.
                    loadAd(this@AlarmTriggeredActivity)
                }
            }
        }

        Admob.gatherUserConsent(this, initializeAds)

        medicinesViewModel = ViewModelProvider(this)[MedicinesViewModel::class.java]

        binding.apply{
            val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
            }else{
                intent?.getParcelableExtra("ALARM_ITEM")
            }

            alarmItem?.let {
                val alarmMillis = localDateTimeToMillis(it.time)

                lifecycleScope.launch(Dispatchers.IO) {
                    val medicinesList = medicinesViewModel.getMedicinesScheduledForTime(alarmMillis)

                    // When there's more than one medicine to be used, instead of showing the
                    // medicine details, user must go to the app and check the medicines.
                    if(medicinesList.size > 1) {
                        withContext(Dispatchers.Main) {
                            hideMedicineDetails(binding)
                            showCheckAppMessage(binding)
                            btnTaken.text = getString(R.string.mark_all_as_used)

                            btnTaken.setOnClickListener {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    withContext(Dispatchers.IO){
                                        medicinesList.forEach { medicine ->
                                            medicine.medicineWasTaken = true
                                            medicinesViewModel.updateMedicines(medicine)
                                            repository.updateMedicinesAsSkipped(medicine.treatmentID, medicine.alarmInMillis)
                                        }
                                    }
                                    dismissNotification(applicationContext, alarmItem.hashCode())
                                    val intent = Intent(applicationContext, MainActivity::class.java)
                                    startActivity(intent)
                                    Admob.showInterstitial(this@AlarmTriggeredActivity)
                                    finish()
                                }
                            }
                        }
                    } else {
                        viewModel.apply {
                            alarmItem.apply {
                                tvAlarmMedicineName.text = medicineName
                                tvAlarmHourMedicine.text = checkDateFormat(alarmHour.toInt(), alarmMinute.toInt(), context = applicationContext)
                                tvAlarmQuantity.text = checkMedicineForm(medicineForm, doseUnit, medicineQuantity, context = applicationContext)

                                ivAlarmMedicineIcon.setImageResource(setMedicineImageView(medicineForm))

                                btnTaken.setOnClickListener{
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        viewModel.markMedicineAsTaken(alarmItem, medicinesViewModel)
                                        val medicine = medicinesList.first()
                                        repository.updateMedicinesAsSkipped(medicine.treatmentID, medicine.alarmInMillis)
                                    }
                                    dismissNotification(applicationContext, alarmItem.hashCode())
                                    val intent = Intent(applicationContext, MainActivity::class.java)
                                    startActivity(intent)
                                    Admob.showInterstitial(this@AlarmTriggeredActivity)
                                    finish()
                                }
                            }
                        }
                    }
                }
            }

            viewModel.apply{
                alarmItem?.apply{
                    btnDismiss.setOnClickListener {
                        //stopMediaPlayer()
                        dismissNotification(applicationContext, alarmItem.hashCode())
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    btnSnooze.setOnClickListener {
                        alarmScheduler.snoozeAlarm(alarmItem)

                        // Alarm was snoozed, there's no need to delivery the follow up alarm
                        lifecycleScope.launch {
                            val alarmItemMillis = localDateTimeToMillis(alarmItem.time)

                            withContext(Dispatchers.IO) {
                                val medicine = repository.getCurrentAlarmData(alarmItemMillis)

                                withContext(Dispatchers.Default){
                                    alarmScheduler.cancelFollowUpAlarm(medicine.hashCode())
                                }
                            }
                        }

                        dismissNotification(applicationContext, alarmItem.hashCode())
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        Admob.showInterstitial(this@AlarmTriggeredActivity)
                        finish()
                    }
                }

                btnSkipDoseActivity.setOnClickListener {
                    if(alarmItem != null) {
                        val alarmMillis = localDateTimeToMillis(alarmItem.time)

                        lifecycleScope.launch(Dispatchers.IO) {
                            val medicines = repository.getMedicinesScheduledForTime(alarmMillis)

                            medicines.forEach {
                                it.wasSkipped = true
                                repository.updateMedicine(it)
                            }
                        }

                        dismissNotification(applicationContext, alarmItem.hashCode())
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        Admob.showInterstitial(this@AlarmTriggeredActivity)
                        finish()
                    }
                }
            }

        }
    }

    private fun setupNotificationAndStatusBar(){
        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            window.statusBarColor = resources.getColor(R.color.blue_gradient_start, null)
            window.navigationBarColor = resources.getColor(R.color.blue_gradient_end, null)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        } else {
            window.statusBarColor = resources.getColor(R.color.blue_gradient_start, null)
            window.navigationBarColor = resources.getColor(R.color.white, null)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true
        }

    }

    private fun dismissNotification(context: Context?, alarmHashCode: Int){
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(alarmHashCode)
        val stopServiceIntent = Intent(context, AlarmService::class.java)
        context?.stopService(stopServiceIntent)
    }

    private fun hideMedicineDetails(binding: ActivityAlarmTriggeredBinding) {
        binding.tvAlarmMedicineName.visibility = View.GONE
        binding.ivAlarmMedicineIcon.visibility = View.GONE
        binding.tv15.visibility = View.GONE
        binding.tvAlarmQuantity.visibility = View.GONE
        binding.tv16.visibility = View.GONE
        binding.tvAlarmHourMedicine.visibility = View.GONE
    }

    private fun showCheckAppMessage(binding: ActivityAlarmTriggeredBinding) {
        binding.textView2.visibility = View.VISIBLE
        binding.imageView4.visibility = View.VISIBLE
    }
}