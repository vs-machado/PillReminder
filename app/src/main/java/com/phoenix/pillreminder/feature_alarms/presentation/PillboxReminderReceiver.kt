package com.phoenix.pillreminder.feature_alarms.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class PillboxReminderReceiver: BroadcastReceiver(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onReceive(context: Context?, intent: Intent?) {
        job = Job()

        launch{
            startAlarmService(context, intent)
        }
    }

    private fun startAlarmService(context: Context?, intent: Intent?){
        val serviceIntent = Intent(context, AlarmService::class.java).apply{
            putExtra("NOTIFICATION_TYPE", "pillbox_reminder")
        }
        ContextCompat.startForegroundService(context!!, serviceIntent)
    }
}