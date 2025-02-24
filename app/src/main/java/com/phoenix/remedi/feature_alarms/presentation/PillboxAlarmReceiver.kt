package com.phoenix.remedi.feature_alarms.presentation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.phoenix.remedi.feature_alarms.domain.model.NotificationType
import com.phoenix.remedi.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.remedi.feature_alarms.presentation.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receiver for delivering pillbox refill reminders notifications.
 * The BroadcastReceiver is triggered when user enables pillbox refill reminders in HomeFragment.
 */
@AndroidEntryPoint
class PillboxAlarmReceiver: BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        val (hours, minutes) = sharedPreferencesRepository.getPillboxReminderHour()
        val isPillboxReminderEnabled = sharedPreferencesRepository.getPillboxPreferences()

        // If user reboots the phone or install an app update the alarm will be rescheduled.
        if(intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            if(isPillboxReminderEnabled && hours != null && minutes != null) {
                alarmScheduler.schedulePillboxReminder(hours, minutes)
                return
            }
        }

        context?.let {
            if(isPillboxReminderEnabled) {
                val notification = NotificationUtils.schedulePillboxDailyReminder(context)
                val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(999, notification)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("NOTIFICATION_TYPE", NotificationType.PILLBOX_REMINDER)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }

        if (isPillboxReminderEnabled && hours != null && minutes != null) {
            alarmScheduler.schedulePillboxReminder(hours, minutes)
        }
    }
}