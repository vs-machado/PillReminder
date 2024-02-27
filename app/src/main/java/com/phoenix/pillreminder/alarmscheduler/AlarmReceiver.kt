package com.phoenix.pillreminder.alarmscheduler

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.AlarmTriggeredActivity

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