package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val HOUR_24_FORMAT = "HH:mm"
private const val HOUR_12_FORMAT = "hh:mm a"

class AlarmTriggeredViewModel : ViewModel() {

     private fun formatHour(hour: Int, minute: Int, pattern: String): String{
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

    fun checkMedicineForm(medicineForm: String, medicineQuantity: String, context: Context): String{
        return when(medicineForm){
            "pill" -> context.getString(R.string.take_pill, medicineQuantity)
            "injection" -> context.getString(R.string.take_injection, medicineQuantity)
            "liquid" ->  context.getString(R.string.take_liquid, medicineQuantity)
            "drop" -> context.getString(R.string.take_drops, medicineQuantity)
            "inhaler" -> context.getString(R.string.inhale, medicineQuantity)
            "pomade" -> context.getString(R.string.apply_pomade, medicineQuantity)
            else -> {""}
        }
    }

    fun setMedicineImageView(medicineForm: String): Int{
        return when(medicineForm){
            "pill" -> R.drawable.ic_pill_coloured
            "pomade" -> R.drawable.ic_ointment
            "injection" -> R.drawable.ic_injection
            "drop" -> R.drawable.ic_dropper
            "inhaler" -> R.drawable.ic_inhalator
            "liquid" -> R.drawable.ic_liquid
            else -> R.drawable.pill_black_and_white
        }
    }

    private fun localDateTimeToMillis(localDateTime: LocalDateTime): Long{
        var millis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        millis -= TimeZone.getDefault().getOffset(millis)

        return millis
    }

    suspend fun markMedicineAsTaken(alarmItem: AlarmItem, medicinesViewModel: MedicinesViewModel) {
        val alarmInMillis = localDateTimeToMillis(alarmItem.time)
        val medicine = medicinesViewModel.getCurrentAlarmData(alarmInMillis)

        if (medicine != null) {
            medicine.medicineWasTaken = true
            medicinesViewModel.updateMedicines(medicine)
        }
    }
}