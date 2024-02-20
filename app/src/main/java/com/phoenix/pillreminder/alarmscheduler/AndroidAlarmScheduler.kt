package com.phoenix.pillreminder.alarmscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.ZoneId

class AndroidAlarmScheduler(private val context: Context): AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleAlarm(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", item.message)
        }

        // Checks if is possible to schedule exact alarms before calling the schedule method
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if(!alarmManager.canScheduleExactAlarms()){
                Log.e("AlarmScheduler", "Cannot schedule exact alarms")
                return
            }
        }

        val alarmTime = item.time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        Log.i("ALARMTIME", "$alarmTime")
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTime,
            PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.i("Alarm", "Alarm set at $alarmTime")
    }

    override fun cancelAlarm(item: AlarmItem) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

}