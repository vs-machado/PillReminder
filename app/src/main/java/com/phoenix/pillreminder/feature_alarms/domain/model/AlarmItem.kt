package com.phoenix.pillreminder.feature_alarms.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
data class AlarmItem(
    val time: LocalDateTime,
    val medicineName: String,
    val medicineForm: String,
    val medicineQuantity: String,
    val alarmHour: String,
    val alarmMinute: String
): Parcelable
