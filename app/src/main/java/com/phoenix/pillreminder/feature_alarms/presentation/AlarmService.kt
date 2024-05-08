package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.presentation.utils.NotificationUtils
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity

class AlarmService: Service() {
    override fun onBind(intent: Intent?): IBinder?{
        return null
    }

    override fun onCreate(){
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        }else{
            intent?.getParcelableExtra("ALARM_ITEM")
        }

        val notification =
            alarmItem?.let { NotificationUtils.createNotification(applicationContext, it) }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
            if (notification != null) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            }
        } else{
            startForeground(1, notification)
        }

        val activityIntent = Intent(this, AlarmTriggeredActivity::class.java).apply{
            putExtra("ALARM_ITEM", alarmItem)
        }
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(activityIntent)

        return START_NOT_STICKY
    }
}