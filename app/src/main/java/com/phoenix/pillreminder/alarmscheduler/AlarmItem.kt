package com.phoenix.pillreminder.alarmscheduler

import java.time.LocalDateTime

data class AlarmItem(
    val time: LocalDateTime,
    val message: String
)
