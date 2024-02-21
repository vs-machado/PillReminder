package com.phoenix.pillreminder.alarmscheduler

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
data class AlarmItem(
    val time: LocalDateTime,
    val message: String,
    val medicineName: String,
    val medicineForm: String,
    val medicineQuantity: String,
    val alarmHour: String,
    val alarmMinute: String
): Parcelable
