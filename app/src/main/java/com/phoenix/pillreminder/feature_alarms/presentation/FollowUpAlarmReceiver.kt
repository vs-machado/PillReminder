package com.phoenix.pillreminder.feature_alarms.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext

class FollowUpAlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback,
    CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job


    override fun onReceive(context: Context?, intent: Intent?) {
        val dao = MedicineDatabase.getInstance(context!!).medicineDao()
        val medicineItem = intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        val alarmTimeInMillis = medicineItem?.time?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        job = Job()

        CoroutineScope(Dispatchers.IO).launch {
            val updatedMedicine = dao.getCurrentAlarmData(alarmTimeInMillis ?: 0)

            if(updatedMedicine?.medicineWasTaken == false && medicineItem != null){
                startAlarmService(context, intent, updatedMedicine)
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