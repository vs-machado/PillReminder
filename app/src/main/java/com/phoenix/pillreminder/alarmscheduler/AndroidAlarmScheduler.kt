package com.phoenix.pillreminder.alarmscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class AndroidAlarmScheduler(private val context: Context): AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleAlarm(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ITEM", item)
        }

        //Log.i("ALARM HASHCODE", "${item.hashCode()}")

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
        Log.i("Alarm", "Alarm set at $alarmTime")
    }

    override fun cancelAlarm(item: AlarmItem, medicine: Medicine) {
        val database = MedicineDatabase.getInstance(context)
        val dao = database.medicineDao()

        CoroutineScope(Dispatchers.IO).launch{
            val currentAlarmInMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val medicineData = dao.getNextAlarmData(item.medicineName, currentAlarmInMillis + 1L) // Search for the next alarm in the database

            val pendingIntent =  PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent == null){
                Log.i("ALARM", "AlarmReceiver is not registered.")
                return@launch
            }
            alarmManager.cancel(pendingIntent)

            if(medicineData?.alarmInMillis != null){
                scheduleNextAlarm(medicineData, context)
            }

            Log.i("ALARM", "ALARM CANCELLED.")
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

}