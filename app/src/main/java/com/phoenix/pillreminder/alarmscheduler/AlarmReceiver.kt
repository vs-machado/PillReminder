package com.phoenix.pillreminder.alarmscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phoenix.pillreminder.activity.AlarmTriggeredActivity

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, AlarmService::class.java)
        context?.startService(serviceIntent)
    }
}