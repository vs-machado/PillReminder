package com.phoenix.remedi.feature_alarms.presentation.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.os.ConfigurationCompat.setLocales
import com.phoenix.remedi.R
import com.phoenix.remedi.feature_alarms.domain.model.AlarmItem
import com.phoenix.remedi.feature_alarms.presentation.AlarmReceiver
import com.phoenix.remedi.feature_alarms.presentation.AlarmService
import com.phoenix.remedi.feature_alarms.presentation.activities.MainActivity
import java.util.Locale

object NotificationUtils {
    private val channelId = "AlarmChannel"
    private val followUpChannelId = "FollowUpAlarmChannel"
    private val pillboxReminderChannelId = "PillboxReminderChannel"
    private val simpleNotificationChannelId = "SimpleNotificationChannel"

    // Used for changing the notification text language.
    // This setting is only used in phones with SDK lower than 33,
    // In versions above, the language is automatically handled by Android OS.
    private fun getLocalizedContext(context: Context): Context {
        // Use getLocalizedContext only for versions before Android 13 (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }

        val compatLocaleList = AppCompatDelegate.getApplicationLocales()

        // Convert LocaleListCompat to LocaleList
        val currentLocale = LocaleList(*compatLocaleList.toLanguageTags().split(",")
            .filter { it.isNotEmpty() }
            .map { Locale.forLanguageTag(it) }
            .toTypedArray())

        return if (currentLocale.isEmpty) {
            context
        } else {
            val config = Configuration(context.resources.configuration).apply {
                setLocales(currentLocale)
            }
            context.createConfigurationContext(config)
        }
    }

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
    fun createNotification(context: Context, item: AlarmItem, hasMultipleAlarmsAtSameTime: Boolean): Notification {
        val localContext = getLocalizedContext(context)
        val alarmUri = Uri.parse("android.resource://" + localContext.packageName + "/" + R.raw.alarm_sound)

        when(Settings.canDrawOverlays(localContext)){
            true -> {
                /* If user give overlay permissions, an activity with medicine informations will be triggered along with a notification.
                The user will mark the medicine as taken on the activity. Notification clicks will lead to the same activity, only opening
                 MainActivity after clicking on "Mark as used" or "Dismiss" button. */

                val notificationIntent = Intent(localContext, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    localContext, item.hashCode(), notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if(hasMultipleAlarmsAtSameTime) {
                    createNotificationChannel(localContext, alarmUri)

                    val title = localContext.getString(R.string.time_to_use_your_medicines)
                    val text = localContext.getString(R.string.check_the_app)

                    return notificationBuilder(localContext, channelId, pendingIntent, title, text)
                }
                else {
                    createNotificationChannel(localContext, alarmUri)

                    return notificationBuilder(localContext, channelId, pendingIntent,
                        localContext.getString(R.string.time_to_use_your_medicine),
                        localContext.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName,
                            checkMedicineForm(item.medicineForm, item.medicineQuantity, item.doseUnit, localContext)))
                }
            }
            false -> {
                /* If user does not give overlay permissions he will mark medicine as used through an action button in notification.
                Notification/action button clicks will lead to MainActivity. */

                val notificationIntent = Intent(localContext, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    localContext, item.hashCode(), notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if(hasMultipleAlarmsAtSameTime) {
                    // Intent for marking medicines as used
                    val markAllUsages = Intent(localContext, AlarmReceiver::class.java).apply{
                        action = localContext.getString(R.string.mark_all_as_used)
                        putExtra("ALARM_ITEM_ACTION", item)
                    }
                    val markAllUsagesPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        localContext, item.hashCode(), markAllUsages, PendingIntent.FLAG_IMMUTABLE
                    )

                    // Intent for snoozing alarms
                    val snoozeAlarmIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                        action = localContext.getString(R.string.snooze_alarm)
                        putExtra("ALARM_ITEM_ACTION", item)
                    }
                    val snoozeAlarmPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        localContext, item.hashCode(),
                        snoozeAlarmIntent, PendingIntent.FLAG_IMMUTABLE
                    )

                    // Intent for skip medicines
                    val skipMedicineIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                        action = localContext.getString(R.string.skip_dose)
                        putExtra("ALARM_ITEM_ACTION", item)
                    }
                    val skipMedicinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        localContext, item.hashCode(),
                        skipMedicineIntent, PendingIntent.FLAG_IMMUTABLE
                    )

                    createNotificationChannel(localContext, alarmUri)

                    val title = localContext.getString(R.string.time_to_use_your_medicines)
                    val text = localContext.getString(R.string.more_than_one_medicine_to_be_used)

                    return notificationBuilderWithActionButtons(localContext, channelId, pendingIntent,
                        title, text, localContext.getString(R.string.mark_all_as_used),
                        markAllUsagesPendingIntent, snoozeAlarmPendingIntent, skipMedicinePendingIntent,
                        localContext.getString(R.string.skip_dose)
                    )
                }
                else {
                    // Intent for marking medicines as used
                    val markAsUsedIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                        action = localContext.getString(R.string.mark_as_used)
                        putExtra("ALARM_ITEM_ACTION", item)
                    }
                    val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        localContext, item.hashCode(),
                        markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
                    )

                    // Intent for snoozing alarms
                    val snoozeAlarmIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                        action = localContext.getString(R.string.snooze_alarm)
                        putExtra("ALARM_ITEM_ACTION", item)
                    }
                    val snoozeAlarmPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        localContext, item.hashCode(),
                        snoozeAlarmIntent, PendingIntent.FLAG_IMMUTABLE
                    )

                    // Intent for skip medicines
                    val skipMedicineIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                        action = localContext.getString(R.string.skip_dose)
                        putExtra("ALARM_ITEM_ACTION", item)
                    }
                    val skipMedicinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        localContext, item.hashCode(),
                        skipMedicineIntent, PendingIntent.FLAG_IMMUTABLE
                    )

                    createNotificationChannel(localContext, alarmUri)

                    val title = localContext.getString(R.string.time_to_use_your_medicine)
                    val text = localContext.getString(R.string.do_not_forget_to_mark_the_medicine_as_taken, item.medicineName, checkMedicineForm(item.medicineForm,
                        item.medicineQuantity, item.doseUnit, localContext))

                    return notificationBuilderWithActionButtons(
                        localContext, channelId, pendingIntent, title, text,
                        localContext.getString(R.string.mark_as_used),
                        markAsUsedPendingIntent, snoozeAlarmPendingIntent,
                        skipMedicinePendingIntent, localContext.getString(R.string.skip_dose)
                    )
                }
            }
        }
    }

     fun createFollowUpNotification(context: Context, item: AlarmItem,
         medicineHashCode: String, hasMultipleAlarmsAtSameTime: Boolean): Notification {
         
         val localContext = getLocalizedContext(context)
         val alarmUri = Uri.parse("android.resource://" + localContext.packageName + "/" + R.raw.alarm_sound)

         val notificationIntent = Intent(localContext, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            localContext, medicineHashCode.toInt(), notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

         when(Settings.canDrawOverlays(localContext)){
             true -> {
                 if (hasMultipleAlarmsAtSameTime) {
                     createFollowUpNotificationChannel(localContext, alarmUri)

                     val title = localContext.getString(R.string.did_you_forget_to_use_your_medicines)
                     val text = localContext.getString(R.string.open_app_pending_medicines)

                     return notificationBuilder(localContext, followUpChannelId, pendingIntent, title, text)
                 }
                 else {
                     createFollowUpNotificationChannel(localContext, alarmUri)

                     val title = localContext.getString(R.string.did_you_forget_to_use_your_medicine, item.medicineName)
                     val text = localContext.getString(R.string.do_not_forget_to_mark_the_medicine_as_used, checkMedicineForm(item.medicineForm,
                         item.medicineQuantity, item.doseUnit, localContext))

                     return notificationBuilder(localContext, followUpChannelId, pendingIntent, title, text)
                 }
             }
             false -> {
                 if (hasMultipleAlarmsAtSameTime) {
                     val markAllUsages = Intent(localContext, AlarmReceiver::class.java).apply{
                         action = localContext.getString(R.string.mark_all_as_used)
                         putExtra("ALARM_ITEM_ACTION", item)
                     }
                     val markAllUsagesPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                         localContext, item.hashCode(), markAllUsages, PendingIntent.FLAG_IMMUTABLE
                     )

                     // Intent for skip medicines
                     val skipMedicineIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                         action = localContext.getString(R.string.skip_dose)
                         putExtra("ALARM_ITEM_ACTION", item)
                     }
                     val skipMedicinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                         localContext, item.hashCode(),
                         skipMedicineIntent, PendingIntent.FLAG_IMMUTABLE
                     )
                     createFollowUpNotificationChannel(localContext, alarmUri)

                     val title = localContext.getString(R.string.did_you_forget_to_use_your_medicines)
                     val text = localContext.getString(R.string.open_app_pending_medicines)

                     return notificationBuilderWithActionButtons(
                         localContext, followUpChannelId, pendingIntent, title,
                         text, localContext.getString(R.string.mark_all_as_used), markAllUsagesPendingIntent,
                         null, skipMedicinePendingIntent, localContext.getString(R.string.skip_dose)
                     )
                 }
                 else {
                     // Intent for marking medicine usage
                     val markAsUsedIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                         action = localContext.getString(R.string.mark_as_used)
                         putExtra("ALARM_ITEM_ACTION", item)
                     }
                     val markAsUsedPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                         localContext, medicineHashCode.toInt(),
                         markAsUsedIntent, PendingIntent.FLAG_IMMUTABLE
                     )
                     // Intent for skip medicines
                     val skipMedicineIntent = Intent(localContext, AlarmReceiver::class.java).apply {
                         action = localContext.getString(R.string.skip_dose)
                         putExtra("ALARM_ITEM_ACTION", item)
                     }
                     val skipMedicinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                         localContext, item.hashCode(),
                         skipMedicineIntent, PendingIntent.FLAG_IMMUTABLE
                     )

                     createFollowUpNotificationChannel(localContext, alarmUri)

                     val title = localContext.getString(R.string.did_you_forget_to_use_your_medicine, item.medicineName)
                     val text = localContext.getString(R.string.do_not_forget_to_mark_the_medicine_as_used, checkMedicineForm(item.medicineForm,
                         item.medicineQuantity, item.doseUnit, localContext))

                     return notificationBuilderWithActionButtons(
                         localContext, followUpChannelId, pendingIntent, title, text,
                         localContext.getString(R.string.mark_as_used), markAsUsedPendingIntent,
                         null, skipMedicinePendingIntent, localContext.getString(R.string.skip_dose)
                     )
                 }
             }
         }
    }

    fun schedulePillboxDailyReminder(context: Context): Notification {
        val localContext = getLocalizedContext(context)
        val notificationIntent = Intent(localContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            localContext, 999, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmUri = Uri.parse("android.resource://" + localContext.packageName + "/" + R.raw.alarm_sound)

        createPillboxNotificationChannel(localContext, alarmUri)

        val title = localContext.getString(R.string.it_s_time_to_refill_your_pillbox)
        val text =
            localContext.getString(R.string.refill_your_pillbox_and_avoid_forgetting_to_use_your_medicine)

        return notificationBuilder(localContext, pillboxReminderChannelId, pendingIntent, title, text)
    }

    // Dummy notification used exclusively for running AlarmService
    fun createSimpleNotification(context: Context, title: String): Notification {
        val localContext = getLocalizedContext(context)
        val notificationIntent = Intent(localContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            localContext, 1, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SimpleNotificationChannel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(simpleNotificationChannelId, name, importance)
            val notificationManager = localContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(localContext, simpleNotificationChannelId)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_pill)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel(context: Context, alarmUri: Uri){
        val localContext = getLocalizedContext(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PillReminderChannel"
            val descriptionText =
                localContext.getString(R.string.channel_for_reminding_users_to_take_their_medicines)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setSound(alarmUri, audioAttributes)
            }
            val notificationManager = localContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun createFollowUpNotificationChannel(context: Context, alarmUri: Uri){
        val localContext = getLocalizedContext(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PillReminderFollowUpChannel"
            val descriptionText =
                localContext.getString(R.string.channel_for_reminding_users_to_take_their_medicines)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val channel = NotificationChannel(followUpChannelId, name, importance).apply {
                description = descriptionText
                setSound(alarmUri, audioAttributes)
            }
            val notificationManager = localContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPillboxNotificationChannel(context: Context, alarmUri: Uri){
        val localContext = getLocalizedContext(context)
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
            val notificationManager = localContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationBuilder(context: Context, channelId: String, pendingIntent: PendingIntent, title: String, text: String): Notification {
        val localContext = getLocalizedContext(context)
        return NotificationCompat.Builder(localContext, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            //.setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_pill)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun notificationBuilderWithActionButtons(
        context: Context, channelId: String, pendingIntent: PendingIntent,
        title: String, content: String,
        actionName: String,
        actionButtonPendingIntent1: PendingIntent,
        snoozeAlarmPendingIntent: PendingIntent? = null,
        skipMedicinePendingIntent: PendingIntent,
        skipMedicineString: String
    ): Notification {
        val localContext = getLocalizedContext(context)
        val largeIcon = BitmapFactory.decodeResource(localContext.resources, R.drawable.ic_pill)

        return NotificationCompat.Builder(localContext, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_pill)
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .addAction(0, skipMedicineString, skipMedicinePendingIntent)
            // Snooze button is not added when the alarm is a follow up alarm (snoozeAlarmPendingIntent == null)
            .apply {
                snoozeAlarmPendingIntent?.let {
                    addAction(0, localContext.getString(R.string.snooze_alarm), snoozeAlarmPendingIntent)
                }
            }
            .addAction(0, actionName, actionButtonPendingIntent1)
            .build()
    }

    private fun checkMedicineForm(medicineForm: String, medicineQuantity: String, doseUnit: String, context: Context): String {
        val localContext = getLocalizedContext(context)
        return when(medicineForm) {
            "pill" -> {
                val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                localContext.resources.getQuantityString(R.plurals.take_pill, quantity, quantity)
            }
            "injection" -> {
                when(doseUnit) {
                    "mL" -> localContext.getString(R.string.take_injection_ml, medicineQuantity)
                    "syringe" -> {
                        val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                        localContext.resources.getQuantityString(R.plurals.take_injection_syringe, quantity, quantity)
                    }
                    else -> throw IllegalArgumentException("Illegal doseUnit value provided")
                }
            }
            "liquid" -> {
                localContext.getString(R.string.take_liquid, medicineQuantity)
            }
            "drop" -> {
                val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                localContext.resources.getQuantityString(R.plurals.take_drops, quantity, quantity)
            }
            "inhaler" -> {
                when(doseUnit) {
                    "mg" -> localContext.getString(R.string.inhale_mg, medicineQuantity)
                    "puff" -> {
                        val quantity = medicineQuantity.toFloatOrNull()?.toInt() ?: 0
                        localContext.resources.getQuantityString(R.plurals.inhale_puff, quantity, quantity)
                    }
                    "mL" -> localContext.getString(R.string.inhale_mL, medicineQuantity)
                    else -> throw IllegalArgumentException("Illegal doseUnit value provided")
                }
            }
            "pomade" -> {
                localContext.getString(R.string.apply_pomade)
            }
            else -> ""
        }
    }

    fun dismissNotification(context: Context) {
        val localContext = getLocalizedContext(context)
        val stopServiceIntent = Intent(localContext, AlarmService::class.java)
        localContext.stopService(stopServiceIntent)
    }
}