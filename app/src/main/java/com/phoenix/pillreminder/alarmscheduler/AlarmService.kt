package com.phoenix.pillreminder.alarmscheduler

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.phoenix.pillreminder.activity.AlarmTriggeredActivity

class AlarmService: Service() {
    override fun onBind(intent: Intent?): IBinder?{
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        val activityIntent = Intent(this, AlarmTriggeredActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(activityIntent)

        return START_NOT_STICKY
    }
}