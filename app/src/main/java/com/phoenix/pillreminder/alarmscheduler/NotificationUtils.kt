package com.phoenix.pillreminder.alarmscheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.AlarmTriggeredActivity

object NotificationUtils {
    fun createNotification(context: Context): Notification {
        val notificationIntent = Intent(context, AlarmTriggeredActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = "AlarmChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "PillReminderChannel"
            val descriptionText = "Channel for reminding users to take their medicines"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Time to take your medicine!")
            .setContentText("Do not forget to mark the medicine as taken.")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .build()
    }
}
