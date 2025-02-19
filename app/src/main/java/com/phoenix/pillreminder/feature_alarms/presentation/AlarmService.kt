package com.phoenix.pillreminder.feature_alarms.presentation

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.activities.AlarmTriggeredActivity
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity
import com.phoenix.pillreminder.feature_alarms.presentation.activities.PillboxReminderActivity
import com.phoenix.pillreminder.feature_alarms.presentation.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
        val notificationType = intent?.getStringExtra("NOTIFICATION_TYPE")

        val alarmItem = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            intent?.getParcelableExtra("ALARM_ITEM", AlarmItem::class.java)
        }else{
            intent?.getParcelableExtra("ALARM_ITEM")
        }

        val hashCode = if (notificationType == "follow_up"){
            intent.getStringExtra("HASH_CODE")
        } else {null}

        when(notificationType){
            "normal" -> {
                serviceScope.launch {
                    alarmItem?.let {
                        val hasMultipleAlarmsAtSameTime = medicineRepository.checkForMultipleAlarmsAtSameTime(
                            alarmItem.alarmHour,
                            alarmItem.alarmMinute
                        )

                        withContext(Dispatchers.Main) {
                            val notification = NotificationUtils.createNotification(applicationContext, alarmItem, hasMultipleAlarmsAtSameTime)

                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                            } else{
                                startForeground(1, notification)
                            }

                            if(Settings.canDrawOverlays(applicationContext)){
                                val activityIntent = Intent(this@AlarmService, AlarmTriggeredActivity::class.java).apply{
                                    putExtra("ALARM_ITEM", alarmItem)
                                }

                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(activityIntent)
                            } else {
                                val activityIntent = Intent(this@AlarmService, MainActivity::class.java).apply{
                                    putExtra("ALARM_ITEM", alarmItem)
                                }
                                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(activityIntent)
                            }
                        }
                    }
                }
            }
            "follow_up" -> {
                serviceScope.launch(Dispatchers.IO) {
                    alarmItem?.let {
                        val hasMultipleAlarmsAtSameTime = medicineRepository.checkForMultipleAlarmsAtSameTime(
                            alarmItem.alarmHour,
                            alarmItem.alarmMinute
                        )

                        withContext(Dispatchers.Main){
                            if(hashCode != null){
                                val notification = NotificationUtils.createFollowUpNotification(applicationContext, alarmItem, hashCode, hasMultipleAlarmsAtSameTime)

                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                                    startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                                } else{
                                    startForeground(1, notification)
                                }

                                if(Settings.canDrawOverlays(applicationContext)){
                                    val activityIntent = Intent(this@AlarmService, AlarmTriggeredActivity::class.java).apply{
                                        putExtra("ALARM_ITEM", alarmItem)
                                    }

                                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(activityIntent)
                                } else {
                                    val activityIntent = Intent(this@AlarmService, MainActivity::class.java).apply{
                                        putExtra("ALARM_ITEM", alarmItem)
                                    }
                                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(activityIntent)
                                }
                            }
                        }
                    }
                }
            }
            "pillboxReminder" -> {
                val notification = NotificationUtils.schedulePillboxDailyReminder(applicationContext)

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                    if (notification != null) {
                        startForeground(999, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    }
                } else{
                    startForeground(999, notification)
                }
                if(Settings.canDrawOverlays(applicationContext)) {
                    val activityIntent = Intent(this, PillboxReminderActivity::class.java)
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(activityIntent)
                } else {
                    val activityIntent = Intent(this, MainActivity::class.java)
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(activityIntent)
                }
            }
        }

        return START_NOT_STICKY
    }

}