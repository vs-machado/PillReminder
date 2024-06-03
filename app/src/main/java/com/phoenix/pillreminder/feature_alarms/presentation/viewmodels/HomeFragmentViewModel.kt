package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmService
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity
import com.phoenix.pillreminder.feature_alarms.presentation.pillboxRequestCode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.UUID

class HomeFragmentViewModel: ViewModel() {
    private var date: Date = Calendar.getInstance().time

    fun setDate(selectedDate: Date){
        date = selectedDate
    }

    fun getDate(): Date{
        return date
    }

    fun cancelWork(workerID: String, context: Context){
        val workRequestID = UUID.fromString(workerID)
        WorkManager.getInstance(context).cancelWorkById(workRequestID)
    }

    fun cancelReminderNotifications(context: Context){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmService::class.java).apply{
            putExtra("NOTIFICATION_TYPE", "pillbox_reminder")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, pillboxRequestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

}