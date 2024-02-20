package com.phoenix.pillreminder.alarmscheduler

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.activity.AlarmTriggeredActivity

class AlarmReceiver: BroadcastReceiver(), ActivityCompat.OnRequestPermissionsResultCallback {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context!!, serviceIntent)

        val builder = NotificationCompat.Builder(context!!, "Notify")
            .setSmallIcon(R.drawable.ic_launcher_background) //Change this by app icon
            .setContentTitle("Time to take your medicine!")
            .setContentText("Do not forget to mark the medicine as taken.")
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("broadcast", "Can't schedule alarm")
            return
        }
        notificationManagerCompat.notify(1, builder.build())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        return
    }

}