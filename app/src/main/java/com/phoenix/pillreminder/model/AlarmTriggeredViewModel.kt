package com.phoenix.pillreminder.model

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val HOUR_24_FORMAT = "HH:mm"
private const val HOUR_12_FORMAT = "hh:mm a"

class AlarmTriggeredViewModel(): ViewModel() {
     fun formatHour(hour: Int, minute: Int, pattern: String): String{
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun checkDateFormat(alarmHour: Int, alarmMinute: Int, context: Context): String{
        return when {
            DateFormat.is24HourFormat(context) -> {
                 formatHour(alarmHour, alarmMinute, HOUR_24_FORMAT)
            }
            //12 hour format
            else -> {
                 formatHour(alarmHour, alarmMinute, HOUR_12_FORMAT)
            }
        }
    }
}