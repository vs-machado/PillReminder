package com.phoenix.pillreminder.alarmscheduler

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.phoenix.pillreminder.activity.AlarmTriggeredActivity

class AlarmService: Service() {
    override fun onBind(intent: Intent?): IBinder?{
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        }else{
            intent?.getParcelableExtra<AlarmItem>("ALARM_ITEM")
        }

        val activityIntent = Intent(this, AlarmTriggeredActivity::class.java).apply{
            putExtra("ALARM_ITEM", alarmItem)
        }
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(activityIntent)

        return START_NOT_STICKY
    }
}