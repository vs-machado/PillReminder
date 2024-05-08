package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity

object NotificationUtils {
    fun createNotification(context: Context, item: AlarmItem): Notification {
        val notificationIntent = Intent(context, AlarmTriggeredActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, item.hashCode(), notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = "AlarmChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "PillReminderChannel"
            val descriptionText =
                context.getString(R.string.channel_for_reminding_users_to_take_their_medicines)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.time_to_take_your_medicine))
            .setContentText(context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .build()
    }
}
