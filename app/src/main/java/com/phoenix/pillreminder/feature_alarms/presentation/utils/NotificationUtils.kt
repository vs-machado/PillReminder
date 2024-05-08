package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity

object NotificationUtils {
    val channelId = "AlarmChannel"
    const val ACTION_MARK_AS_USED = "Mark as used"
    fun createNotification(context: Context, item: AlarmItem): Notification {
        when(Settings.canDrawOverlays(context)){
            true -> {
                /* If user give overlay permissions, an activity with medicine informations will be triggered along with a notification.
                The user will mark the medicine as taken on the activity. Notification clicks will lead to the same activity, only opening
                 MainActivity after clicking on "Mark as used" or "Dismiss" button. */

                val notificationIntent = Intent(context, AlarmTriggeredActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, item.hashCode(), notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                createNotificationChannel(context)

                return notificationBuilder(context, channelId, pendingIntent)
            }
            false -> {
                /* If user does not give overlay permissions he will mark medicine as used through an action button in notification.
                Notification/action button clicks will lead to MainActivity. */

                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, item.hashCode(), notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val markAsUsedIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_MARK_AS_USED
                    putExtra("ALARM_ITEM_ACTION", item)
                }
                val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, item.hashCode(),
                    markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
                )

                createNotificationChannel(context)

                return notificationBuilderWithActionButtons(context, channelId, pendingIntent, markAsUsedPendingIntent)
            }
        }
    }

    private fun createNotificationChannel(context: Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun notificationBuilder(context: Context, channelId: String, pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.time_to_take_your_medicine))
            .setContentText(context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun notificationBuilderWithActionButtons(context: Context, channelId: String, pendingIntent: PendingIntent, actionButtonPendingIntent1: PendingIntent): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.time_to_take_your_medicine))
            .setContentText(context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(0, ACTION_MARK_AS_USED, actionButtonPendingIntent1)
            .build()
    }
}
