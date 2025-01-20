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
import androidx.core.app.NotificationCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity

object NotificationUtils {
    private val channelId = "AlarmChannel"
    private val followUpChannelId = "FollowUpAlarmChannel"
    private val pillboxReminderChannelId = "PillboxReminderChannel"

    /**
     * Creates a notification for the alarm.
     * Alarms can be delivered to the user in two ways:
     * 1. Notification with actions to mark medicine usage or snoozing alarms
     * 2. Notification (only for draw user attention) and activity screen with medicine information and user options.
     *
     * @param context Application context
     * @param item Item containing all alarm details.
     *
     * @see [AlarmItem]
     */
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
                    context.getString(R.string.time_to_use_your_medicine),
                    context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName,
                        checkMedicineForm(item.medicineForm, item.medicineQuantity, item.doseUnit, context)))
            }
            false -> {
                /* If user does not give overlay permissions he will mark medicine as used through an action button in notification.
                Notification/action button clicks will lead to MainActivity. */

                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, item.hashCode(), notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Intent for marking medicines as used
                val markAsUsedIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = context.getString(R.string.mark_as_used)
                    putExtra("ALARM_ITEM_ACTION", item)
                }
                val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, item.hashCode(),
                    markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
                )

                // Intent for snoozing alarms
                val snoozeAlarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = context.getString(R.string.snooze_alarm)
                    putExtra("ALARM_ITEM_ACTION", item)
                }
                val snoozeAlarmPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, item.hashCode(),
                    snoozeAlarmIntent, PendingIntent.FLAG_IMMUTABLE
                )

                createNotificationChannel(context, alarmUri)

                val title = context.getString(R.string.time_to_use_your_medicine)
                val text = context.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName, checkMedicineForm(item.medicineForm,
                    item.medicineQuantity, item.doseUnit, context))

                return notificationBuilderWithActionButtons(
                    context, channelId, pendingIntent, title, text,
                    markAsUsedPendingIntent, snoozeAlarmPendingIntent
                )
            }
        }
    }

     fun createFollowUpNotification(context: Context, item: AlarmItem, medicineHashCode: String): Notification {
         val alarmUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.alarm_sound)

         val notificationIntent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context, medicineHashCode.toInt(), notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

         // Intent for marking medicine usage
        val markAsUsedIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = context.getString(R.string.mark_as_used)
            putExtra("ALARM_ITEM_ACTION", item)
        }
        val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, medicineHashCode.toInt(),
            markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
        )

         createFollowUpNotificationChannel(context, alarmUri)

         val title = context.getString(R.string.did_you_forget_to_use_your_medicine, item.medicineName)
         val text = context.getString(R.string.do_not_forget_to_mark_the_medicine_as_used, checkMedicineForm(item.medicineForm,
             item.medicineQuantity, item.doseUnit, context))

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
        val alarmUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.alarm_sound)

        createPillboxNotificationChannel(context, alarmUri)

        val title = context.getString(R.string.it_s_time_to_refill_your_pillbox)
        val text =
            context.getString(R.string.refill_your_pillbox_and_avoid_forgetting_to_use_your_medicine)

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
    private fun createFollowUpNotificationChannel(context: Context, alarmUri: Uri){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PillReminderFollowUpChannel"
            val descriptionText =
                context.getString(R.string.channel_for_reminding_users_to_take_their_medicines)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val channel = NotificationChannel(followUpChannelId, name, importance).apply {
                description = descriptionText
                setSound(alarmUri, audioAttributes)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPillboxNotificationChannel(context: Context, alarmUri: Uri){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "PillboxReminderChannel"
            val descriptionText = "Channel for reminding users to refill their pillboxes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val channel = NotificationChannel(pillboxReminderChannelId, name, importance).apply{
                description = descriptionText
                setSound(alarmUri, audioAttributes)
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
        actionButtonPendingIntent1: PendingIntent,
        snoozeAlarmPendingIntent: PendingIntent? = null
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
            // Snooze button is not added when the alarm is a follow up alarm (snoozeAlarmPendingIntent == null)
            .apply {
                snoozeAlarmPendingIntent?.let {
                    addAction(0, context.getString(R.string.snooze_alarm), snoozeAlarmPendingIntent)
                }
            }
            .build()
    }

    private fun checkMedicineForm(medicineForm: String, medicineQuantity: String, doseUnit: String, context: Context): String {
        return when(medicineForm) {
            "pill" -> {
                val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                context.resources.getQuantityString(R.plurals.take_pill, quantity, quantity)
            }
            "injection" -> {
                when(doseUnit) {
                    "mL" -> context.getString(R.string.take_injection_ml, medicineQuantity)
                    "syringe" -> {
                        val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                        context.resources.getQuantityString(R.plurals.take_injection_syringe, quantity, quantity)
                    }
                    else -> throw IllegalArgumentException("Illegal doseUnit value provided")
                }
            }
            "liquid" -> {
                context.getString(R.string.take_liquid, medicineQuantity)
            }
            "drop" -> {
                val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                context.resources.getQuantityString(R.plurals.take_drops, quantity, quantity)
            }
            "inhaler" -> {
                when(doseUnit) {
                    "mg" -> context.getString(R.string.inhale_mg, medicineQuantity)
                    "puff" -> {
                        val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                        context.resources.getQuantityString(R.plurals.inhale_puff, quantity, quantity)
                    }
                    "mL" -> context.getString(R.string.inhale_mL, medicineQuantity)
                    else -> throw IllegalArgumentException("Illegal doseUnit value provided")
                }
            }
            "pomade" -> {
                context.getString(R.string.apply_pomade)
            }
            else -> ""
        }
    }
}
