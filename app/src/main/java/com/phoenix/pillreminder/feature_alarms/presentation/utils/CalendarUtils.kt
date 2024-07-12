package com.phoenix.pillreminder.feature_alarms.presentation.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarUtils {
     fun formatHour(hour: Int, minute: Int, pattern: String): String{
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(calendar.time)
    }
}