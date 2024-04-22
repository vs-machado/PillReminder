package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import androidx.lifecycle.ViewModel
import java.util.Calendar

class AlarmHourViewModel: ViewModel() {
    private val calendar = Calendar.getInstance()

    fun getCurrentHour(): Int{
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun getCurrentMinute(): Int{
        return calendar.get(Calendar.MINUTE)
    }
}