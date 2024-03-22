package com.phoenix.pillreminder.alarmscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDatabase
import com.phoenix.pillreminder.model.MedicinesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext

class AlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback,
    CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onReceive(context: Context?, intent: Intent?) {
        val alarmItem = intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        val database = MedicineDatabase.getInstance(context!!)
        val dao = database.medicineDao()
        job = Job()

        launch {
            val medicineData = dao.getNextAlarmData(alarmItem!!.medicineName, System.currentTimeMillis())

            if (medicineData?.alarmInMillis != null) {
                scheduleNextAlarm(medicineData, context)
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
        }
        ContextCompat.startForegroundService(context!!, serviceIntent)
    }

    private fun scheduleNextAlarm(medicine: Medicine, context: Context?){
        val alarmScheduler: AlarmScheduler = AndroidAlarmScheduler(context!!)

        val alarmItem = AlarmItem(
            time = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            medicineName = medicine.name,
            medicineForm = medicine.form,
            medicineQuantity = medicine.quantity.toString(),
            alarmHour = medicine.alarmHour.toString(),
            alarmMinute = medicine.alarmMinute.toString()
        )

        alarmItem?.let(alarmScheduler::scheduleAlarm)
    }

}