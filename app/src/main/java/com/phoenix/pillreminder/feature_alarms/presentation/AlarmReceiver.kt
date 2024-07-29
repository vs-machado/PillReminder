package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.TimeZone
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback, CoroutineScope {

    @Inject
    lateinit var repository: MedicineRepository

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onReceive(context: Context?, intent: Intent?) {
        val alarmItem = intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        val alarmItemAction = intent?.getParcelableExtra("ALARM_ITEM_ACTION", AlarmItem::class.java)
        val actionString = context?.getString(R.string.mark_as_used)
        job = Job()

        // Automatically reschedule alarms if user reboots the device or install an app update
        if(intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED){
            if (context != null) {
                rescheduleAlarmsOnBoot(context)
            }
            return
        }
        if(intent?.action == actionString && alarmItemAction != null){
            markMedicineAsTaken(alarmItemAction, repository)

            //Notification dismissal after pressing button
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(alarmItemAction.hashCode())
            val stopServiceIntent = Intent(context, AlarmService::class.java)
            context?.stopService(stopServiceIntent)

            return
        }

        launch {
            val alarmScheduler = context?.let { AndroidAlarmScheduler(repository, it) }


            //Example: If interval between alarms is equal to 1 hour a notification will be sent after 15 minutes if user don't mark the medicine as used
            if(alarmItem?.time != null) {
                val alarmItemMillis = localDateTimeToMillis(alarmItem.time)
                val followUpTime = System.currentTimeMillis() + aQuarterIntervalBetweenAlarms(alarmItem.medicineName, alarmItemMillis, repository)
                val medicine = repository.getCurrentAlarmData(alarmItemMillis)

                if(medicine != null){
                    alarmScheduler?.scheduleFollowUpAlarm(medicine, alarmItem, followUpTime)
                }
            }

            //When an alarm is received by system the next alarm is automatically scheduled
            val medicineData = alarmItem?.let { repository.getNextAlarmData(it.medicineName, System.currentTimeMillis()) }
            if (medicineData?.alarmInMillis != null) {
                alarmScheduler?.scheduleNextAlarm(medicineData)
            }


        }

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
        job = Job()

        launch{
            val medicineAlarmsToSchedule = repository.getAlarmsToRescheduleAfterReboot(System.currentTimeMillis())
            val alarmScheduler = AndroidAlarmScheduler(repository, context)

            medicineAlarmsToSchedule.forEach{ medicine ->
                alarmScheduler.scheduleNextAlarm(medicine)
            }
        }
    }

    private fun markMedicineAsTaken(alarmItem: AlarmItem, repository: MedicineRepository) {
        CoroutineScope(Dispatchers.IO).launch{
            val alarmInMillis = localDateTimeToMillis(alarmItem.time)
            val medicine = repository.getCurrentAlarmData(alarmInMillis)

            if (medicine != null) {
                medicine.medicineWasTaken = true
                repository.updateMedicine(medicine)
            }
        }
    }
    private fun localDateTimeToMillis(localDateTime: LocalDateTime): Long{
        var millis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        millis -= TimeZone.getDefault().getOffset(millis)

        return millis
    }

    private suspend fun aQuarterIntervalBetweenAlarms(medicineName: String, alarmInMillis: Long, repository: MedicineRepository): Long{
        return withContext(Dispatchers.IO) {
            val nextAlarm = repository.getNextAlarmData(medicineName, alarmInMillis)

            if (nextAlarm != null) {
                (nextAlarm.alarmInMillis - alarmInMillis) / 4
            } else {
                0L
            }
        }
    }
    }