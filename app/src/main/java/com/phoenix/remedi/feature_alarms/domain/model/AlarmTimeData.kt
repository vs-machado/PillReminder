package com.phoenix.remedi.feature_alarms.domain.model

import androidx.room.ColumnInfo

data class AlarmTimeData (
    @ColumnInfo(name = "alarm_hour") val hour: Int,
    @ColumnInfo(name = "alarm_minute") val minute: Int
)