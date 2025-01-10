package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityAlarmTriggeredBinding
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.utils.DateUtil.localDateTimeToMillis
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.AlarmTriggeredViewModel
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

        medicinesViewModel = ViewModelProvider(this)[MedicinesViewModel::class.java]

        val alarmReceiver = AlarmReceiver()

        binding.apply{
            val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
            }else{
                intent?.getParcelableExtra("ALARM_ITEM")
            }

            viewModel.apply{
                alarmItem?.apply{
                    tvAlarmMedicineName.text = medicineName
                    tvAlarmHourMedicine.text = checkDateFormat(alarmHour.toInt(), alarmMinute.toInt(), context = applicationContext)
                    tvAlarmQuantity.text = checkMedicineForm(medicineForm, doseUnit, medicineQuantity, context = applicationContext)

                    ivAlarmMedicineIcon.setImageResource(setMedicineImageView(medicineForm))

                    btnTaken.setOnClickListener{
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.markMedicineAsTaken(alarmItem, medicinesViewModel)
                        }
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    btnDismiss.setOnClickListener {
                        //stopMediaPlayer()
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    btnSnooze.setOnClickListener {
                        alarmReceiver.snoozeAlarm(alarmItem, repository, applicationContext)

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

                        alarmReceiver.dismissNotification(applicationContext, alarmItem.hashCode())
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
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
}