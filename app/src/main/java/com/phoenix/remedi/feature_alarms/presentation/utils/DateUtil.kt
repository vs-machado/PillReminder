package com.phoenix.remedi.feature_alarms.presentation.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    fun localDateTimeToMillis(localDateTime: LocalDateTime): Long {
        var millis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        millis -= TimeZone.getDefault().getOffset(millis)
        return millis
    }
}