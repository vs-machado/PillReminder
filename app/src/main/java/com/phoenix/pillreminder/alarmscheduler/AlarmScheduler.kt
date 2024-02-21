package com.phoenix.pillreminder.alarmscheduler

interface AlarmScheduler {
    fun scheduleAlarm(item: AlarmItem)
    fun cancelAlarm(item: AlarmItem)
}