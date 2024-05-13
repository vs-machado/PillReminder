package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
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

    fun createNotification(context: Context, item: AlarmItem): Notification {
        val alarmUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.alarm_sound)

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
                createNotificationChannel(context, alarmUri)

                return notificationBuilder(context, channelId, pendingIntent, item)
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
                    action = context.getString(R.string.mark_as_used)
                    putExtra("ALARM_ITEM_ACTION", item)
                }
                val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, item.hashCode(),
                    markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
                )

                createNotificationChannel(context, alarmUri)

                return notificationBuilderWithActionButtons(context, channelId, pendingIntent, markAsUsedPendingIntent, item)
            }
        }
    }

    private fun createNotificationChannel(context: Context, alarmUri: Uri){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PillReminderChannel"
            val descriptionText =
                context.getString(R.string.channel_for_reminding_users_to_take_their_medicines)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setSound(alarmUri, audioAttributes)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationBuilder(context: Context, channelId: String, pendingIntent: PendingIntent, item: AlarmItem): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.time_to_take_your_medicine))
            .setContentText(context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName, checkMedicineForm(item.medicineForm,
                item.medicineQuantity, context)))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun notificationBuilderWithActionButtons(context: Context, channelId: String, pendingIntent: PendingIntent,
                                                     actionButtonPendingIntent1: PendingIntent, item: AlarmItem): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.time_to_take_your_medicine))
            .setContentText(context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName, checkMedicineForm(item.medicineForm,
                item.medicineQuantity, context)))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.mark_as_used), actionButtonPendingIntent1)
            .build()
    }

    private fun checkMedicineForm(medicineForm: String, medicineQuantity: String, context: Context): String{
        return when(medicineForm){
            "pill" -> context.getString(R.string.take_pill, medicineQuantity)
            "injection" -> context.getString(R.string.take_injection, medicineQuantity)
            "liquid" ->  context.getString(R.string.take_liquid, medicineQuantity)
            "drop" -> context.getString(R.string.take_drops, medicineQuantity)
            "inhaler" -> context.getString(R.string.inhale, medicineQuantity)
            "pomade" -> context.getString(R.string.apply_pomade)
            else -> {""}
        }
    }
}
