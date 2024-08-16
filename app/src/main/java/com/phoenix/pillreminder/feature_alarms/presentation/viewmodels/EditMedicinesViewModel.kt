package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.utils.CalendarUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val repository: MedicineRepository,
    private val alarmScheduler: AndroidAlarmScheduler
): ViewModel() {

    suspend fun getMillisList(medicineName: String): List<Long>{
        return withContext(Dispatchers.IO){
            repository.getAllAlarmsMillis(medicineName, System.currentTimeMillis())
        }
    }

    fun convertMillisToAlarmHourList(longList: List<Long>, pattern: String): List<AlarmHour>{
        // A set is used to avoid repeated hours (because different millis can lead to the same day hour.)
        val uniqueAlarmStrings = mutableSetOf<String>()

        return longList.mapNotNull{ millis ->
            val dateString = CalendarUtils.formatMillisToString(millis, pattern)

            if(uniqueAlarmStrings.add(dateString)){
                AlarmHour(dateString)
            } else {
                null
            }
        }
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