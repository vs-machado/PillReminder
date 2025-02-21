package com.phoenix.remedi.feature_alarms.presentation.utils

import android.content.Context
import android.text.format.DateFormat.is24HourFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

    // When user's phone is in 12-hour format, convert 24-hour to 12-hour format
    fun formatStringHourList(hours: List<String>, context: Context): List<String> {
        val inputPattern = "HH:mm" // Database input is in 24-hour format
        val inputFormat = SimpleDateFormat(inputPattern, Locale.getDefault())

        val targetPattern = if(is24HourFormat(context)){
            "HH:mm"
        } else {
            "hh:mm a"
        }

        val targetFormat = SimpleDateFormat(targetPattern, Locale.getDefault())

        return hours.mapNotNull { hour ->
            try {
                val date = inputFormat.parse(hour)
                date?.let { targetFormat.format(it) }
            } catch (e: Exception) {
                null
            }
        }
    }


    fun formatMillisToString(millis: Long, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(millis))
    }
}