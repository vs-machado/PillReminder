package com.phoenix.pillreminder.alarmscheduler

import com.phoenix.pillreminder.db.Medicine

interface AlarmScheduler {
    fun scheduleAlarm(item: AlarmItem)

    fun cancelAlarm(item: AlarmItem, cancelAll: Boolean)
}