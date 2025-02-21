package com.phoenix.remedi.feature_alarms.domain.repository

import androidx.lifecycle.LiveData
import com.phoenix.remedi.feature_alarms.domain.model.AlarmTimeData
import com.phoenix.remedi.feature_alarms.domain.model.Medicine

interface MedicineRepository {
    suspend fun insertMedicines(medicines: List<Medicine>)

    suspend fun updateMedicine(medicine: Medicine)

    suspend fun updateMedicinesActiveStatus(medicineName: String, currentTimeMillis: Long, isActive: Boolean)

    suspend fun updateExpiredMedicines(treatmentID: String, name: String, quantity: Float, form: String, endDate: Long, frequency: String,
                                       currentTime: Long)

    suspend fun deleteMedicine(medicine: Medicine)

    suspend fun deleteAllSelectedMedicines(medicines: List<Medicine>)

    suspend fun getSelectedDaysList(medicineName: String, treatmentID: String): String

    suspend fun deleteUpcomingAlarms(medicineName: String, currentTimeMillis: Long)

    suspend fun getAlarmsAfterProvidedMillis(medicineName: String, millis: Long): List<Medicine>

    suspend fun getAlarmTimeSinceMidnight(medicineName: String): Long

    fun getAllMedicines(): LiveData<List<Medicine>>

    suspend fun getDailyAlarms(medicineName: String, alarmsPerDay: Int, treatmentID: String): List<AlarmTimeData>

    suspend fun getMedicineEditTimestamp(medicineName: String): Long

    fun getAllMedicinesWithSameName(medicineName: String): List<Medicine>

    fun getMedicines(): List<Medicine>

    fun getWorkerID(medicineName: String, treatmentID: String): String

    fun getCurrentAlarmData(alarmInMillis: Long): Medicine?

    suspend fun getNextAlarmData(medicineName: String, currentTimeMillis: Long): Medicine?

    suspend fun getFirstMedicineOfNextDay(nextDayInMillis: Long): Medicine?

    suspend fun getFirstMedicineOfTheDay(millis: Long): Medicine?

    suspend fun hasNextAlarmData(medicineName: String, currentTimeMillis: Long): Boolean

    fun getAlarmsToRescheduleAfterReboot(currentTimeMillis: Long): List<Medicine>

    fun getAlarmsToRescheduleEveryMonth(medicineName: String, alarmsPerDay: Int): List<Medicine>

    fun getLastAlarmFromAllDistinctMedicines(): List<Medicine>

    suspend fun getAlarmTimesForMedicine(medicineName: String, cutoffTime: Long, treatmentID: String): List<String>

    suspend fun getLastAlarm(medicineName: String, treatmentID: String): Medicine

    suspend fun updateMedicinesAsSkipped(treatmentID: String, alarmInMillis: Long)

    suspend fun checkForMultipleAlarmsAtSameTime(hour: String, minute: String): Boolean

    suspend fun getMedicinesScheduledForTime(timeInMillis: Long): List<Medicine>
}