package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity
import com.phoenix.pillreminder.feature_alarms.presentation.utils.NotificationUtils

class AlarmService: Service() {
    override fun onBind(intent: Intent?): IBinder?{
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        val notificationType = intent?.getStringExtra("NOTIFICATION_TYPE")

        val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        }else{
            intent?.getParcelableExtra("ALARM_ITEM")
        }

        val hashCode = if (notificationType == "follow_up"){
            intent.getStringExtra("HASH_CODE")
        } else {null}

        when(notificationType){
            "normal" -> {
                val notification =
                    alarmItem?.let { NotificationUtils.createNotification(applicationContext, it) }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                    if (notification != null) {
                        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    }
                } else{
                    startForeground(1, notification)
                }
                if(Settings.canDrawOverlays(applicationContext)){
                    val activityIntent = Intent(this, AlarmTriggeredActivity::class.java).apply{
                        putExtra("ALARM_ITEM", alarmItem)
                    }

                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(activityIntent)
                } else {
                    val activityIntent = Intent(this, MainActivity::class.java).apply{
                        putExtra("ALARM_ITEM", alarmItem)
                    }
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(activityIntent)
                }
            }
            "follow_up" -> {
                if(hashCode != null){
                    val notification = alarmItem?.let{ NotificationUtils.createFollowUpNotification(applicationContext, it, hashCode)}

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                        if (notification != null) {
                            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                        }
                    } else{
                        startForeground(1, notification)
                    }
                    val activityIntent = Intent(this, MainActivity::class.java).apply{
                        putExtra("ALARM_ITEM", alarmItem)
                    }
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(activityIntent)
                }

            }
        }

        return START_NOT_STICKY
    }
}