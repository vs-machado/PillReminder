package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.utils.CalendarUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class EditMedicinesViewModel @Inject constructor(
    private val alarmScheduler: AndroidAlarmScheduler
): ViewModel() {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    fun setInitialized(bool: Boolean) {
        _isInitialized.value = bool
    }

    fun convertMillisToAlarmHourList(context: Context, alarmHourList: List<Pair<Int, Int>>): List<AlarmHour>{
        val is24HourFormat = DateFormat.is24HourFormat(context)
        val pattern = if(is24HourFormat) "HH:mm" else "hh:mm a"

        return alarmHourList
            .distinct()
            .sortedBy { normalizeIntPairToMillis(it) }
            .map{ AlarmHour(CalendarUtils.formatMillisToString(normalizeIntPairToMillis(it), pattern)) }
    }

    fun resetCalendarHourToMidnight(millis: Long, timeZone: TimeZone): Long {
        val timeZoneOffset = timeZone.getOffset(millis)
        val date = Calendar.getInstance().apply {
            timeInMillis = millis - timeZoneOffset
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return date.timeInMillis
    }

    private fun normalizeIntPairToMillis(alarms: Pair<Int, Int>): Long {
        val calendar = Calendar.getInstance().apply{
            set(Calendar.HOUR_OF_DAY, alarms.first)
            set(Calendar.MINUTE, alarms.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }

    // It sums the selected alarm milliseconds to the selected date millis. This is used to set
    // the treatment end date.
    fun formatSelectedDateWithOffset(selectedDateMillis: Long, alarmMillis: Long, timeZone: TimeZone): Long {
        val timeZoneOffset = timeZone.getOffset(selectedDateMillis)
        val date = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis - timeZoneOffset
            timeInMillis += alarmMillis
        }

        return date.timeInMillis
    }

    fun cancelAlarm(medicine: Medicine, cancelAll: Boolean){
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.unit,
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        viewModelScope.launch(Dispatchers.Default) {
            alarmScheduler.cancelAlarm(alarmItem, cancelAll)
        }
    }

    fun parseAlarmTime(alarmHourString: String): Pair<Int, Int>{
        val formatter = DateTimeFormatter.ofPattern("[h:mm a][H:mm]")

        val localTime = LocalTime.parse(alarmHourString.trim().uppercase(), formatter)
        return Pair(localTime.hour, localTime.minute)
    }

    fun formatTime(hourOfDay: Int, minute: Int, is24HourFormat: Boolean): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
        }

        val format = if (is24HourFormat) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(calendar.time)
    }



}