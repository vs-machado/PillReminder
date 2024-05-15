package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDao
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.TimeZone
import kotlin.coroutines.CoroutineContext

class AlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback,
    CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onReceive(context: Context?, intent: Intent?) {
        val dao = MedicineDatabase.getInstance(context!!).medicineDao()
        val alarmItem = intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        val alarmItemAction = intent?.getParcelableExtra("ALARM_ITEM_ACTION", AlarmItem::class.java)
        job = Job()

        if(intent?.action == Intent.ACTION_BOOT_COMPLETED){
            rescheduleAlarmsOnBoot(context)
            return
        }
        if(intent?.action == "Mark as used" && alarmItemAction != null){
            markMedicineAsTaken(alarmItemAction, dao)
            Log.i("alarmItem", "$alarmItemAction")

            //Notification dismissal after pressing button
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(alarmItemAction.hashCode())
            val stopServiceIntent = Intent(context, AlarmService::class.java)
            context.stopService(stopServiceIntent)

            return
        }

        launch {
            val alarmScheduler = AndroidAlarmScheduler(context)

            Log.d("ALARM ITEM", "${alarmItem?.time}")
            //Example: If interval between alarms is equal to 1 hour a notification will be sent after 15 minutes if user don't mark the medicine as used
            if(alarmItem?.time != null) {
                Log.d("ALARM ITEM", "not null")
                val alarmItemMillis = localDateTimeToMillis(alarmItem.time)
                val followUpTime = System.currentTimeMillis() + aQuarterIntervalBetweenAlarms(alarmItem.medicineName, alarmItemMillis, dao)
                val medicine = dao.getCurrentAlarmData(alarmItemMillis)

                if(medicine != null){
                    Log.d("Alarm", "medicine: $medicine, alarmItem: $alarmItem, followUpTime: $followUpTime")
                    alarmScheduler.scheduleFollowUpAlarm(medicine, alarmItem, followUpTime)
                    Log.d("Alarm", "scheduleFollowUpAlarm being called")
                }
            }

            //When an alarm is received by system the next alarm is automatically scheduled
            val medicineData = alarmItem?.let { dao.getNextAlarmData(it.medicineName, System.currentTimeMillis()) }
            if (medicineData?.alarmInMillis != null) {
                alarmScheduler.scheduleNextAlarm(medicineData, context)
            }


        }

        Log.d("AlarmReceiver", "AlarmItem received: $alarmItem")
        startAlarmService(context, intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        return
    }

    private fun startAlarmService(context: Context?, intent: Intent?){
        val serviceIntent = Intent(context, AlarmService::class.java).apply{
            putExtra("ALARM_ITEM", intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java))
            putExtra("NOTIFICATION_TYPE", "normal")
        }
        ContextCompat.startForegroundService(context!!, serviceIntent)
    }

    private fun rescheduleAlarmsOnBoot(context: Context) {
        val database = MedicineDatabase.getInstance(context)
        val dao = database.medicineDao()
        job = Job()

        launch{
            val medicineAlarmsToSchedule = dao.getAlarmsToRescheduleAfterReboot(System.currentTimeMillis())
            val alarmScheduler = AndroidAlarmScheduler(context)

            medicineAlarmsToSchedule.forEach{ medicine ->
                alarmScheduler.scheduleNextAlarm(medicine, context)
            }
        }
    }

    private fun markMedicineAsTaken(alarmItem: AlarmItem, dao: MedicineDao) {
        CoroutineScope(Dispatchers.IO).launch{
            val alarmInMillis = localDateTimeToMillis(alarmItem.time)
            val medicine = dao.getCurrentAlarmData(alarmInMillis)
            Log.i("ALARM", "$medicine")
            if (medicine != null) {
                Log.i("ALARM", "Medicine not null")
                medicine.medicineWasTaken = true
                dao.updateMedicine(medicine)
            }
        }
    }
    private fun localDateTimeToMillis(localDateTime: LocalDateTime): Long{
        var millis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        millis -= TimeZone.getDefault().getOffset(millis)

        return millis
    }

    private suspend fun aQuarterIntervalBetweenAlarms(medicineName: String, alarmInMillis: Long, dao: MedicineDao): Long{
        return withContext(Dispatchers.IO) {
            val nextAlarm = dao.getNextAlarmData(medicineName, alarmInMillis)
            Log.d("Alarm nextAlarm", "${nextAlarm?.alarmInMillis}")

            if (nextAlarm != null) {
                (nextAlarm.alarmInMillis - alarmInMillis) / 4
            } else {
                0L
            }
        }
    }
    }