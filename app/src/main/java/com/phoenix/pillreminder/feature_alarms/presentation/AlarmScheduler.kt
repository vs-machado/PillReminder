package com.phoenix.pillreminder.feature_alarms.presentation

import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine

interface AlarmScheduler {
    fun scheduleAlarm(item: AlarmItem)

    fun scheduleFollowUpAlarm(medicine: Medicine, item: AlarmItem, followUpTime: Long)

    fun schedulePillboxReminder(hours: Int, minutes: Int)

    fun scheduleNextAlarm(medicine: Medicine)

    fun snoozeAlarm(item: AlarmItem)

    suspend fun cancelAlarm(item: AlarmItem, cancelAll: Boolean)

    fun cancelFollowUpAlarm(hashCode: Int)

    fun cancelPillboxReminder(): Boolean
}