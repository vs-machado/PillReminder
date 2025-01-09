package com.phoenix.pillreminder.feature_alarms.presentation

import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine

interface AlarmScheduler {
    fun scheduleAlarm(item: AlarmItem)

    fun schedulePillboxReminder(hours: Int, minutes: Int)

    fun snoozeAlarm(item: AlarmItem, snoozeMinutes: Int)

    suspend fun cancelAlarm(item: AlarmItem, cancelAll: Boolean)
}