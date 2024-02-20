package com.phoenix.pillreminder.alarmscheduler

import java.time.LocalDateTime

data class AlarmItem(
    val time: LocalDateTime,
    val message: String,
    val medicineName: String,
    val medicineForm: String,
    val medicineQuantity: String,
    val alarmHour: String,
    val alarmMinute: String
)
