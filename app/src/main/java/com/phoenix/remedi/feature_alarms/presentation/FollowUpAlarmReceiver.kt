package com.phoenix.remedi.feature_alarms.presentation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.remedi.feature_alarms.domain.model.AlarmItem
import com.phoenix.remedi.feature_alarms.domain.model.NotificationType
import com.phoenix.remedi.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.remedi.feature_alarms.presentation.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
* This class is responsible for receiving the follow-up alarm that reminder users when
 * they already received an alarm but not marked the medicine usage.
*/
@AndroidEntryPoint
class FollowUpAlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback,
    CoroutineScope {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    private lateinit var firstPendingMedicineAlarmItem: AlarmItem

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job


    // Receives the follow-up alarm. If the medicine was used, the alarm service does not start.
    // If any of the medicines triggered by the alarm was not marked as used or skipped,
    // it schedules a new follow up alarm.
    override fun onReceive(context: Context?, intent: Intent?) {

        val medicineItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        } else {
            intent?.getParcelableExtra("ALARM_ITEM")
        }

        val alarmTimeInMillis = medicineItem?.time?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

        launch(Dispatchers.IO) {
            alarmTimeInMillis?.let {
                val updatedMedicine = repository.getCurrentAlarmData(alarmTimeInMillis)
                val medicinesList = updatedMedicine?.let { it1 ->
                    repository.getMedicinesScheduledForTime(
                        it1.alarmInMillis)
                }

                // Check if any medicine in the list hasn't been taken
                val anyMedicineNotTakenNorSkipped = medicinesList?.any { medicine ->
                    !medicine.medicineWasTaken && !medicine.wasSkipped
                }

                // Finds the first medicine that was not used nor skipped.
                // If user does not mark the medicine usage or skip it, another follow up alarm will be scheduled with the medicine data.
                val firstPendingMedicine = medicinesList?.find { medicine ->
                    !medicine.medicineWasTaken && !medicine.wasSkipped
                }
                firstPendingMedicine?.let {
                     firstPendingMedicineAlarmItem = AlarmItem(
                        Instant.ofEpochMilli(firstPendingMedicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        firstPendingMedicine.name,
                        firstPendingMedicine.form,
                        firstPendingMedicine.quantity.toString(),
                        firstPendingMedicine.unit,
                        firstPendingMedicine.alarmHour.toString(),
                        firstPendingMedicine.alarmMinute.toString()
                    )
                    val followUpTime = System.currentTimeMillis() + 1000 * 60 * 10L

                    if(anyMedicineNotTakenNorSkipped == true) {
                        alarmScheduler.scheduleFollowUpAlarm(firstPendingMedicine, firstPendingMedicineAlarmItem, followUpTime)
                    }
                }

                // Determines if the alarm service will start or not.
                if(anyMedicineNotTakenNorSkipped == true){
                    context?.let {
                        val hasMultipleAlarmsAtSameTime = repository.checkForMultipleAlarmsAtSameTime(
                            medicineItem.alarmHour,
                            medicineItem.alarmMinute
                        )
                        if(firstPendingMedicine != null) {
                            val notification = NotificationUtils.createFollowUpNotification(context, firstPendingMedicineAlarmItem,
                                firstPendingMedicineAlarmItem.hashCode().toString(), hasMultipleAlarmsAtSameTime)
                            val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(firstPendingMedicineAlarmItem.hashCode(), notification)

                            startAlarmService(context, intent)
                        }
                    }
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
            putExtra("NOTIFICATION_TYPE", NotificationType.FOLLOWUP as Parcelable)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra("ALARM_ITEM", intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java))
            } else {
                putExtra("ALARM_ITEM", intent?.getParcelableExtra("ALARM_ITEM") as AlarmItem?)
            }
        }

        ContextCompat.startForegroundService(context!!, serviceIntent)
    }

}