package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity

object NotificationUtils {
    private val channelId = "AlarmChannel"
    private val followUpChannelId = "FollowUpAlarmChannel"
    private val pillboxReminderChannelId = "PillboxReminderChannel"

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

                return notificationBuilder(context, channelId, pendingIntent,
                    context.getString(R.string.time_to_take_your_medicine),
                    context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName,
                        checkMedicineForm(item.medicineForm, item.medicineQuantity, context)))
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

                val title = context.getString(R.string.time_to_take_your_medicine)
                val text = context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName, checkMedicineForm(item.medicineForm,
                    item.medicineQuantity, context))

                return notificationBuilderWithActionButtons(
                    context, channelId, pendingIntent, title, text,
                    markAsUsedPendingIntent
                )
            }
        }
    }

     fun createFollowUpNotification(context: Context, item: AlarmItem, medicineHashCode: String): Notification{
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, medicineHashCode.toInt(), notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val markAsUsedIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = context.getString(R.string.mark_as_used)
            putExtra("ALARM_ITEM_ACTION", item)
        }
        val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, medicineHashCode.toInt(),
            markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel(context)

         val title = context.getString(R.string.did_you_forget_to_use_your_medicine, item.medicineName)
         val text = context.getString(R.string.do_not_forget_to_mark_the_medicine_as_used, checkMedicineForm(item.medicineForm,
             item.medicineQuantity, context))

        return notificationBuilderWithActionButtons(
            context, followUpChannelId, pendingIntent, title, text,
            markAsUsedPendingIntent
        )
    }

    fun schedulePillboxDailyReminder(context: Context): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 999, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        createPillboxNotificationChannel(context)

        val title = context.getString(R.string.it_s_time_to_refill_your_pillbox)
        val text =
            context.getString(R.string.refill_your_pillbox_and_avoid_forgetting_to_take_your_medication)
        Log.d("Alarm", "notificationutils")

        return notificationBuilder(context, pillboxReminderChannelId, pendingIntent, title, text)
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
    private fun createNotificationChannel(context: Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PillReminderFollowUpChannel"
            val descriptionText =
                context.getString(R.string.channel_for_reminding_users_to_take_their_medicines)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(followUpChannelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPillboxNotificationChannel(context: Context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "PillboxReminderChannel"
            val descriptionText = "Channel for reminding users to refill their pillboxes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(pillboxReminderChannelId, name, importance).apply{
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationBuilder(context: Context, channelId: String, pendingIntent: PendingIntent, title: String, text: String): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            //.setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_pill)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun notificationBuilderWithActionButtons(
        context: Context, channelId: String, pendingIntent: PendingIntent,
        title: String, content: String,
        actionButtonPendingIntent1: PendingIntent
    ): Notification {
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_pill)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_pill)
            .setLargeIcon(largeIcon)
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
