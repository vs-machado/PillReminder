package com.phoenix.pillreminder.feature_alarms.data.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoenix.pillreminder.feature_alarms.presentation.utils.NotificationUtils

class PillboxReminderWorker(appContext: Context,
                            params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val notification = NotificationUtils.schedulePillboxDailyReminder(applicationContext)

        if (checkNotificationPermission(applicationContext)) {
            try {
                // Show the notification
                NotificationManagerCompat.from(applicationContext).notify(999, notification)
            } catch (e: SecurityException) {
                Log.d("Error", "Failed to show notification", e)
                return Result.failure()
            }
        } else {
            //Already handled post notifications in appintro activity
            return Result.failure()
        }
        Log.d("Alarm", "worker dowork")

        return Result.success()
    }

    private fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

}