package com.phoenix.pillreminder.feature_alarms.domain.repository

import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem

interface AlarmScheduler {
    fun scheduleAlarm(item: AlarmItem)

    fun cancelAlarm(item: AlarmItem, cancelAll: Boolean)
}