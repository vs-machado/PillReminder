package com.phoenix.pillreminder.alarmscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.db.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
            val alarmScheduler = AndroidAlarmScheduler(context)

            if (medicineData?.alarmInMillis != null) {
                alarmScheduler.scheduleNextAlarm(medicineData, context)
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

}