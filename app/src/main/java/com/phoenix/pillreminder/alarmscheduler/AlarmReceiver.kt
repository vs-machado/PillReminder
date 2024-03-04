package com.phoenix.pillreminder.alarmscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                putExtra("ALARM_ITEM", intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java))
            }else{
                putExtra("ALARM_ITEM", intent?.getParcelableExtra<AlarmItem>("ALARM_ITEM"))
            }
        }
        ContextCompat.startForegroundService(context!!, serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        return
    }

}