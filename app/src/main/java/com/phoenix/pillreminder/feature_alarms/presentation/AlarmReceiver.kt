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
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.utils.DateUtil.localDateTimeToMillis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

const val TEN_MINUTES = 1000 * 60 * 10L


@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback, CoroutineScope {

    private lateinit var job: Job
    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onReceive(context: Context?, intent: Intent?) {
        val alarmItem = intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        val alarmItemAction = intent?.getParcelableExtra("ALARM_ITEM_ACTION", AlarmItem::class.java)
        val actionMarkAsUsed = context?.getString(R.string.mark_as_used)
        val actionSnoozeAlarm = context?.getString(R.string.snooze_alarm)
        job = Job()

        // Automatically reschedule alarms if user reboots the device or install an app update
        if(intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent?.action == "com.phoenix.pillreminder.RESCHEDULEBACKUPALARMS"){
            if (context != null) {
                rescheduleAlarms()
            }
            return
        }
        if(intent?.action == actionMarkAsUsed && alarmItemAction != null){
            markMedicineAsTaken(alarmItemAction, repository)
            dismissNotification(context, alarmItemAction.hashCode())
            return
        }
        if(intent?.action == actionSnoozeAlarm && alarmItemAction != null) {
            alarmScheduler.snoozeAlarm(alarmItemAction)

            // Alarm was snoozed, there's no need to delivery the follow up alarm
            launch {
                val alarmItemMillis = localDateTimeToMillis(alarmItemAction.time)
                val medicine = repository.getCurrentAlarmData(alarmItemMillis)
                alarmScheduler.cancelFollowUpAlarm(medicine.hashCode())
            }

            dismissNotification(context, alarmItemAction.hashCode())
            return
        }

        launch {
            if(alarmItem?.time != null) {
                val alarmItemMillis = localDateTimeToMillis(alarmItem.time)
                val followUpTime = System.currentTimeMillis() + getFollowUpNotificationInterval(alarmItem.medicineName, alarmItemMillis, repository)
                val medicine = repository.getCurrentAlarmData(alarmItemMillis)

                if(medicine != null){
                    /* A follow up alarm will be scheduled and triggered if user does not mark the medicine usage.
                       The follow up alarm will trigger after 1/4 of the time between the current alarm and the next alarm.
                       If the interval is greater than 10 minutes, the follow up alarm will trigger after 10 minutes.*/
                    alarmScheduler.scheduleFollowUpAlarm(medicine, alarmItem, followUpTime)

                    // The notification is only delivered if user does not confirm the medicine usage
                    if(!medicine.medicineWasTaken){
                        startAlarmService(context, intent)
                    }

                    //When an alarm is received by system the next alarm is automatically scheduled
                    val medicineData =
                        alarmItem.let { repository.getNextAlarmData(it.medicineName, System.currentTimeMillis()) }
                    if (medicineData?.alarmInMillis != null) {
                        alarmScheduler.scheduleNextAlarm(medicineData)
                        return@launch
                    }

                    // When there's no remaining alarms to be scheduled the treatment period has ended
                    repository.updateMedicinesActiveStatus(medicine.name, System.currentTimeMillis(), false)
                }
            }
        }
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

    private fun rescheduleAlarms() {
        job = Job()

        launch{
            val medicineAlarmsToSchedule = repository.getAlarmsToRescheduleAfterReboot(System.currentTimeMillis())

            medicineAlarmsToSchedule.forEach{ medicine ->
                alarmScheduler.scheduleNextAlarm(medicine)
            }
        }
    }

    private fun markMedicineAsTaken(alarmItem: AlarmItem, repository: MedicineRepository) {
        val job = CoroutineScope(Dispatchers.IO).launch{
            val alarmInMillis = localDateTimeToMillis(alarmItem.time)
            val medicine = repository.getCurrentAlarmData(alarmInMillis)

            if (medicine != null) {
                // Marks the medicine usage
                medicine.medicineWasTaken = true
                repository.updateMedicine(medicine)

                // Update the previous medicines not marked as used as skipped
                repository.updateMedicinesAsSkipped(medicine.treatmentID, medicine.alarmInMillis)
            }
        }

        job.invokeOnCompletion {
            job.cancel()
        }
    }

    private suspend fun getFollowUpNotificationInterval(medicineName: String, alarmInMillis: Long, repository: MedicineRepository): Long {
        val nextAlarm = repository.getNextAlarmData(medicineName, alarmInMillis)

        return if (nextAlarm != null) {
            val intervalBetweenAlarms = nextAlarm.alarmInMillis - alarmInMillis

            if(intervalBetweenAlarms >= TEN_MINUTES) TEN_MINUTES else intervalBetweenAlarms / 4

        } else {
            TEN_MINUTES
        }
    }

    /**
     * Notification dismissal after pressing button
     *
     * @param context App context
     * @param alarmHashCode Hashcode of notification that needs to be cancelled
     */
    private fun dismissNotification(context: Context?, alarmHashCode: Int){
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(alarmHashCode)
        val stopServiceIntent = Intent(context, AlarmService::class.java)
        context?.stopService(stopServiceIntent)
    }
}