package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.repository.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

const val pillboxRequestCode: Int = 999

class AndroidAlarmScheduler(private val context: Context): AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleAlarm(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
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
                context,
                item.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

    }

    override fun cancelAlarm(item: AlarmItem, cancelAll: Boolean) {
        val database = MedicineDatabase.getInstance(context)
        val dao = database.medicineDao()
        val itemTimeInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        CoroutineScope(Dispatchers.IO).launch{
            val pendingIntent =  PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent == null || itemTimeInMillis < System.currentTimeMillis()){
                val alarmAlreadyScheduled = dao.getNextAlarmData(item.medicineName, System.currentTimeMillis())
                val alarmItemTime = alarmAlreadyScheduled?.alarmInMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                }
                if (alarmItemTime != null){
                    val alarmItem = AlarmItem(
                        alarmItemTime,
                        alarmAlreadyScheduled.name,
                        alarmAlreadyScheduled.form,
                        alarmAlreadyScheduled.quantity.toString(),
                        alarmAlreadyScheduled.alarmHour.toString(),
                        alarmAlreadyScheduled.alarmMinute.toString()
                    )

                    cancelAlarm(alarmItem, true)
                }
                return@launch
            }

            if(!cancelAll){
                alarmManager.cancel(pendingIntent)
                val currentAlarmInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Search for the alarm (next to the selected) in the database
                val nextAlarm = dao.getNextAlarmData(item.medicineName, currentAlarmInMillis + 1L)

                if(nextAlarm?.alarmInMillis != null){
                    scheduleNextAlarm(nextAlarm, context)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    fun scheduleNextAlarm(medicine: Medicine, context: Context?){
        val alarmScheduler: AlarmScheduler = AndroidAlarmScheduler(context!!)

        val alarmItem = AlarmItem(
            time = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            medicineName = medicine.name,
            medicineForm = medicine.form,
            medicineQuantity = medicine.quantity.toString(),
            alarmHour = medicine.alarmHour.toString(),
            alarmMinute = medicine.alarmMinute.toString()
        )

        alarmItem.let(alarmScheduler::scheduleAlarm)
    }

    override fun schedulePillboxReminder(hours: Int, minutes: Int) {
        val intent = Intent(context, PillboxReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillboxRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            getTimeInMillisForAlarm(hours, minutes),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleFollowUpAlarm(medicine: Medicine, item: AlarmItem, followUpTime: Long){
        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            putExtra("ALARM_ITEM", item)
        }

        //The first alarm requestCode is based on AlarmItem hashcode. The follow up alarm is based on Medicine hashcode.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
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