package com.phoenix.pillreminder.feature_alarms.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
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

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job


    // Receives the follow-up alarm. If the medicine was used, the alarm service does not start.
    // If any of the medicines triggered by the alarm was not marked as used or skipped,
    // it schedules a new follow up alarm.
    override fun onReceive(context: Context?, intent: Intent?) {
        val medicineItem = intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
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
                    val firstPendingMedicineAlarmItem = AlarmItem(
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
                    startAlarmService(context, intent, updatedMedicine)
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

    private fun startAlarmService(context: Context?, intent: Intent?, updatedMedicine: Medicine){
        val serviceIntent = Intent(context, AlarmService::class.java).apply{
            putExtra("ALARM_ITEM", intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java))
            putExtra("NOTIFICATION_TYPE", "follow_up")
            putExtra("HASH_CODE", updatedMedicine.hashCode().toString())
        }
        ContextCompat.startForegroundService(context!!, serviceIntent)
    }

}