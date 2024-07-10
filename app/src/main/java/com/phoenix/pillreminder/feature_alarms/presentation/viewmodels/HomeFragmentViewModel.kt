package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.work.WorkManager
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
        WorkManager.getInstance(context).cancelUniqueWork("PillboxReminder")
        Log.d("Alarm", "work cancelled")

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(999)
    }

}