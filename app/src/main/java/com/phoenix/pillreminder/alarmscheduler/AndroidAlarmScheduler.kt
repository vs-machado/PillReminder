package com.phoenix.pillreminder.alarmscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId

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
        //Log.i("Alarm", "Alarm set at $alarmTime")
    }

    override fun cancelAlarm(item: AlarmItem, cancelAll: Boolean) {
        val database = MedicineDatabase.getInstance(context)
        val dao = database.medicineDao()
        val itemTimeInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        runBlocking{
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
                return@runBlocking
            }

            if(!cancelAll){
                val currentAlarmInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Search for the alarm (next to the selected) in the database
                val nextAlarm = dao.getNextAlarmData(item.medicineName, currentAlarmInMillis + 1L)

                if(nextAlarm?.alarmInMillis != null){
                    scheduleNextAlarm(nextAlarm, context)
                }
                return@runBlocking
            }

            alarmManager.cancel(pendingIntent)
            Toast.makeText(context,
                "ALARM CANCELLED: ${item.time}",
                Toast.LENGTH_LONG).show()

            //Log.i("ALARM", "ALARM CANCELLED: ${item.time}")
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
        //Log.i("ALARM", "ALARM SCHEDULED: ${alarmItem.time}")
    }

}