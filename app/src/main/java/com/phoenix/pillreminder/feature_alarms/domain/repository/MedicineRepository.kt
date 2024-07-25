package com.phoenix.pillreminder.feature_alarms.domain.repository

import androidx.lifecycle.LiveData
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine

interface MedicineRepository {
    suspend fun insertMedicines(medicines: List<Medicine>)

    suspend fun updateMedicine(medicine: Medicine)

    suspend fun deleteMedicine(medicine: Medicine)

    suspend fun deleteAllSelectedMedicines(medicines: List<Medicine>)

    fun getAllMedicines(): LiveData<List<Medicine>>

    fun getAllMedicinesWithSameName(medicineName: String): List<Medicine>

    fun getMedicines(): List<Medicine>

    fun getWorkerID(medicineName: String): String

    fun getCurrentAlarmData(alarmInMillis: Long): Medicine?

    suspend fun getNextAlarmData(medicineName: String, currentTimeMillis: Long): Medicine?

    suspend fun getFirstMedicineOfNextDay(nextDayInMillis: Long): Medicine?

    suspend fun getFirstMedicineOfTheDay(millis: Long): Medicine?

    suspend fun hasNextAlarmData(medicineName: String, currentTimeMillis: Long): Boolean

    fun getAlarmsToRescheduleAfterReboot(currentTimeMillis: Long): List<Medicine>

    fun getAlarmsToRescheduleEveryMonth(medicineName: String, alarmsPerDay: Int): List<Medicine>

    fun getAllDistinctMedicines(): List<Medicine>

    suspend fun getAlarmTimesForMedicine(medicineName: String): List<String>
}