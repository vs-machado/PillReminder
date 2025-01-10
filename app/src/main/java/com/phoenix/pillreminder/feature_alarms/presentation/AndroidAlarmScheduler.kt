package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.data.worker.PillboxReminderWorker
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AndroidAlarmScheduler @Inject constructor(
    private val medicineRepository: MedicineRepository,
   @ApplicationContext private val appContext: Context
): AlarmScheduler {
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun scheduleAlarm(item: AlarmItem) {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ITEM", item)
        }

        // Checks if is possible to schedule exact alarms before calling the schedule method
        if(!alarmManager.canScheduleExactAlarms()){
            Log.e("AlarmScheduler", "Cannot schedule exact alarms")
            return
        }

        val alarmTime = item.time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTime,
            PendingIntent.getBroadcast(
                appContext,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

    }

    /**
     * Snoozes a triggered alarm.
     * The snooze duration will be configurable by users in settings in the future.
     *
     * @param item AlarmItem to be snoozed
     * @param snoozeMinutes Number of minutes to snooze the alarm
     */
    override fun snoozeAlarm(item: AlarmItem, snoozeMinutes: Int) {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ITEM", item)
        }

        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozeTime,
            PendingIntent.getBroadcast(
                appContext,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        )
    }

    /** Cancel one or all alarms for a specific medicine.
     *
     *  When user deletes all alarms (cancelAll = true), the method will search for a valid pendingIntent,
     *  cancelling the alarm even if the medicine object clicked was not scheduled yet.
     *
     *  @param item AlarmItem containing medicine info
     *  @param cancelAll If true, cancels all future alarms for the given medicine.
     */
    override suspend fun cancelAlarm(item: AlarmItem, cancelAll: Boolean) {
        withContext(Dispatchers.Default) {
            when(cancelAll){
                true -> {
                    var currentItem = item
                    var cutoffTime = System.currentTimeMillis()

                    while(true) {
                        val itemTimeInMillis = currentItem.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                        val pendingIntent = PendingIntent.getBroadcast(
                            appContext,
                            currentItem.hashCode(),
                            Intent(appContext, AlarmReceiver::class.java),
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                        )

                        if (pendingIntent == null || itemTimeInMillis < System.currentTimeMillis()) {
                            val nextAlarm = withContext(Dispatchers.IO) {
                                medicineRepository.getNextAlarmData(currentItem.medicineName, cutoffTime)
                            }

                            if (nextAlarm == null) {
                                break
                            }

                            val alarmItemTime = nextAlarm.alarmInMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            }

                            if (alarmItemTime != null) {
                                currentItem = AlarmItem(
                                    alarmItemTime,
                                    nextAlarm.name,
                                    nextAlarm.form,
                                    nextAlarm.quantity.toString(),
                                    nextAlarm.unit,
                                    nextAlarm.alarmHour.toString(),
                                    nextAlarm.alarmMinute.toString()
                                )
                                cutoffTime = nextAlarm.alarmInMillis
                            } else {
                                break
                            }
                        }
                        else {
                            cancelAlarmForItem(currentItem)
                            break
                        }
                    }
                }
                false -> {
                    val pendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        item.hashCode(),
                        Intent(appContext, AlarmReceiver::class.java),
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (pendingIntent != null) {
                        val alarmCancelled = cancelAlarmForItem(item)

                        if (alarmCancelled) {
                            val currentAlarmInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                            val nextAlarm = withContext(Dispatchers.IO) {
                                medicineRepository.getNextAlarmData(item.medicineName, currentAlarmInMillis + 1L)
                            }

                            if (nextAlarm?.alarmInMillis != null) {
                                scheduleNextAlarm(nextAlarm)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cancelAlarmForItem(item: AlarmItem): Boolean {
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            item.hashCode(),
            Intent(appContext, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            return true
        }

        return false
    }

    /**
     * Use to cancel follow-up alarms.
     * Used when user snoozes an alarm, so there's no need to deliver the follow-up alarm.
     *
     * @param hashCode alarm hashcode used to cancel the alarm
     */
    override fun cancelFollowUpAlarm(hashCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            hashCode,
            Intent(appContext, FollowUpAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }


    fun scheduleNextAlarm(medicine: Medicine){
        val alarmScheduler: AlarmScheduler = AndroidAlarmScheduler(medicineRepository, appContext)

        val alarmItem = AlarmItem(
            time = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            medicineName = medicine.name,
            medicineForm = medicine.form,
            medicineQuantity = medicine.quantity.toString(),
            doseUnit = medicine.unit,
            alarmHour = medicine.alarmHour.toString(),
            alarmMinute = medicine.alarmMinute.toString()
        )
        alarmItem.let(alarmScheduler::scheduleAlarm)
    }

    override fun schedulePillboxReminder(hours: Int, minutes: Int) {
        val workRequest = PeriodicWorkRequestBuilder<PillboxReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(getInitialDelay(hours,minutes), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            "PillboxReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun scheduleFollowUpAlarm(medicine: Medicine, item: AlarmItem, followUpTime: Long){
        val intent = Intent(appContext, FollowUpAlarmReceiver::class.java).apply {
            putExtra("ALARM_ITEM", item)
        }

        //The first alarm requestCode is based on AlarmItem hashcode. The follow up alarm is based on Medicine hashcode.
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            medicine.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("debug", "alarm scheduled")
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            followUpTime,
            pendingIntent
        )
    }

    private fun getInitialDelay(hours: Int, minutes: Int): Long {
        val now = System.currentTimeMillis()
        val desiredTime = getTimeInMillisForAlarm(hours, minutes)
        return if (desiredTime > now) {
            desiredTime - now
        } else {
            desiredTime + 24 * 60 * 60 * 1000 - now // Schedule for next day
        }
    }

    private fun getTimeInMillisForAlarm(hours: Int, minutes: Int): Long{
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis

        val totalMinutes = (hours * 60) + minutes
        val userProvidedMillis = totalMinutes * 60000L

        val userCalendar = Calendar.getInstance()

        userCalendar.apply{
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis += userProvidedMillis
        }

        //When user sets refill reminders at a time of day that has already passed, the notifications start being sent the next day
        return if (calendar.timeInMillis < userCalendar.timeInMillis){
            userCalendar.set(Calendar.HOUR_OF_DAY, hours)
            userCalendar.set(Calendar.MINUTE, minutes)
            userCalendar.set(Calendar.SECOND, 0)
            userCalendar.set(Calendar.MILLISECOND, 0)
            userCalendar.timeInMillis
        } else {
            userCalendar.add(Calendar.DAY_OF_YEAR, 1)
            userCalendar.set(Calendar.HOUR_OF_DAY, hours)
            userCalendar.set(Calendar.MINUTE, minutes)
            userCalendar.set(Calendar.SECOND, 0)
            userCalendar.set(Calendar.MILLISECOND, 0)
            userCalendar.timeInMillis
        }
    }

}