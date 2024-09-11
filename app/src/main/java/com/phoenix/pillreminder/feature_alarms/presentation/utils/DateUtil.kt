package com.phoenix.pillreminder.feature_alarms.presentation.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtil {
    fun millisToDateString(text: String?, millis: Long): String {
        val date = Date(millis)
        val dateFormat =
            SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Locale.getDefault())

        return if (text != null) {
            "$text ${dateFormat.format(date)}"
        } else {
            dateFormat.format(date)
        }
    }
}