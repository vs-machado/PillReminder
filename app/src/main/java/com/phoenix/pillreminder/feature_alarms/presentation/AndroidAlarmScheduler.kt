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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        Log.d("debug", "intent: $intent")

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

    override suspend fun cancelAlarm(item: AlarmItem, cancelAll: Boolean) {
        val itemTimeInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

       withContext(Dispatchers.Default){
            val pendingIntent =  PendingIntent.getBroadcast(
                appContext,
                item.hashCode(),
                Intent(appContext, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent == null || itemTimeInMillis < System.currentTimeMillis()){
                withContext(Dispatchers.IO){
                    val alarmAlreadyScheduled = medicineRepository.getNextAlarmData(item.medicineName, System.currentTimeMillis())

                    withContext(Dispatchers.Default){
                        val alarmItemTime = alarmAlreadyScheduled?.alarmInMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                        }
                        if (alarmItemTime != null){
                            val alarmItem = AlarmItem(
                                alarmItemTime,
                                alarmAlreadyScheduled.name,
                                alarmAlreadyScheduled.form,
                                alarmAlreadyScheduled.quantity.toString(),
                                alarmAlreadyScheduled.unit,
                                alarmAlreadyScheduled.alarmHour.toString(),
                                alarmAlreadyScheduled.alarmMinute.toString()
                            )

                            cancelAlarm(alarmItem, true)
                        }
                    }
                }
                return@withContext
            }

            if(!cancelAll){
                alarmManager.cancel(pendingIntent)
                val currentAlarmInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Search for the alarm (next to the selected) in the database
                val nextAlarm = medicineRepository.getNextAlarmData(item.medicineName, currentAlarmInMillis + 1L)

                if(nextAlarm?.alarmInMillis != null){
                    scheduleNextAlarm(nextAlarm)
                }
                return@withContext
            }

            withContext(Dispatchers.Main) {
                alarmManager.cancel(pendingIntent)
            }
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
        Log.d("alarmItem", "schedulenextalarm alarmitem: $alarmItem")
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