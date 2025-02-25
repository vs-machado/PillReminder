package com.phoenix.remedi.feature_alarms.presentation

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.phoenix.remedi.R
import com.phoenix.remedi.feature_alarms.domain.model.AlarmItem
import com.phoenix.remedi.feature_alarms.domain.model.NotificationType
import com.phoenix.remedi.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.remedi.feature_alarms.presentation.activities.AlarmTriggeredActivity
import com.phoenix.remedi.feature_alarms.presentation.activities.PillboxReminderActivity
import com.phoenix.remedi.feature_alarms.presentation.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Service responsible for handling medication alarms and displaying them to the user.
 * 
 * This service creates a foreground notification and starts the appropriate activity
 * to show medication information. It operates based on the following flow:
 * 
 * 1. Starts a foreground notification with "Opening the app..." message in the user's language
 * 2. If overlay permissions are granted:
 *    - Opens AlarmTriggeredActivity with the alarm details
 * 3. Stops itself after launching the activity
 *
 */
@AndroidEntryPoint
class AlarmService: Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    @Inject lateinit var medicineRepository: MedicineRepository

    override fun onBind(intent: Intent?): IBinder?{
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        val notificationType = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("NOTIFICATION_TYPE", NotificationType::class.java)
        } else {
            intent?.getParcelableExtra("NOTIFICATION_TYPE") as NotificationType?
        }

        val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        }else{
            intent?.getParcelableExtra("ALARM_ITEM")
        }

        val notification = NotificationUtils.createSimpleNotification(
            applicationContext,
            getString(R.string.opening_app)
        )
        
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else{
            startForeground(1, notification)
        }

        when(notificationType) {
            NotificationType.NORMAL, NotificationType.FOLLOWUP -> {
                if(Settings.canDrawOverlays(applicationContext)) {
                    val activityIntent = Intent(this@AlarmService, AlarmTriggeredActivity::class.java).apply {
                        putExtra("ALARM_ITEM", alarmItem)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        activityIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    try {
                        pendingIntent.send()
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                    }
                }
            }

            NotificationType.PILLBOX_REMINDER -> {
                if(Settings.canDrawOverlays(applicationContext)) {
                    val activityIntent = Intent(this@AlarmService, PillboxReminderActivity::class.java).apply {
                        putExtra("ALARM_ITEM", alarmItem)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        1, // different request code for this type
                        activityIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    try {
                        pendingIntent.send()
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                    }
                }
            }
            null -> {}
        }
        stopSelf()

        return START_NOT_STICKY
    }

}