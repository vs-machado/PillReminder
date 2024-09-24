package com.phoenix.pillreminder.feature_alarms.data.repository

import androidx.lifecycle.LiveData
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDao
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository

class MedicineRepositoryImpl (
    private val dao: MedicineDao
): MedicineRepository {
    override suspend fun insertMedicines(medicines: List<Medicine>) {
        dao.insertMedicines(medicines)
    }

    override suspend fun updateMedicine(medicine: Medicine) {
        dao.updateMedicine(medicine)
    }

    override suspend fun updateExpiredMedicines(
        treatmentID: String,
        name: String,
        quantity: Float,
        form: String,
        endDate: Long,
        frequency: String,
        currentTime: Long
    ) {
        dao.updateExpiredMedicines(treatmentID, name, quantity, form, endDate, frequency, currentTime)
    }

    override suspend fun getSelectedDaysList(
        medicineName: String,
        treatmentID: String
    ): String {
        return dao.getSelectedDaysList(medicineName, treatmentID)
    }

    override suspend fun deleteMedicine(medicine: Medicine) {
        dao.deleteMedicine(medicine)
    }

    override suspend fun deleteAllSelectedMedicines(medicines: List<Medicine>) {
        dao.deleteAllSelectedMedicines(medicines)
    }

    override suspend fun getAlarmsAfterProvidedMillis(medicineName: String, millis: Long): List<Medicine> {
        return dao.getAlarmsAfterProvidedMillis(medicineName, millis)
    }

    override suspend fun getAlarmTimeSinceMidnight(medicineName: String): Long{
        return dao.getAlarmTimeSinceMidnight(medicineName)
    }

    override fun getAllMedicines(): LiveData<List<Medicine>> {
        return dao.getAllMedicines()
    }

    override suspend fun getDailyAlarms(medicineName: String, alarmsPerDay: Int, treatmentID: String): List<Long> {
        return dao.getDailyAlarms(medicineName, alarmsPerDay, treatmentID)
    }

    override suspend fun getMedicineEditTimestamp(medicineName: String): Long{
        return dao.getMedicineEditTimestamp(medicineName)
    }


    override fun getAllMedicinesWithSameName(medicineName: String): List<Medicine> {
        return dao.getAllMedicinesWithSameName(medicineName)
    }

    override fun getMedicines(): List<Medicine> {
        return dao.getMedicines()
    }

    override fun getWorkerID(medicineName: String): String {
        return dao.getWorkerID(medicineName)
    }

    override fun getCurrentAlarmData(alarmInMillis: Long): Medicine? {
        return dao.getCurrentAlarmData(alarmInMillis)
    }

    override suspend fun getNextAlarmData(
        medicineName: String,
        currentTimeMillis: Long
    ): Medicine? {
        return dao.getNextAlarmData(medicineName, currentTimeMillis)
    }

    override suspend fun getFirstMedicineOfNextDay(nextDayInMillis: Long): Medicine? {
        return dao.getFirstMedicineOfNextDay(nextDayInMillis)
    }

    override suspend fun getFirstMedicineOfTheDay(millis: Long): Medicine? {
        return dao.getFirstMedicineOfTheDay(millis)
    }

    override suspend fun hasNextAlarmData(medicineName: String, currentTimeMillis: Long): Boolean {
        return dao.hasNextAlarmData(medicineName, currentTimeMillis)
    }

    override fun getAlarmsToRescheduleAfterReboot(currentTimeMillis: Long): List<Medicine> {
        return dao.getAlarmsToRescheduleAfterReboot(currentTimeMillis)
    }

    override fun getAlarmsToRescheduleEveryMonth(
        medicineName: String,
        alarmsPerDay: Int
    ): List<Medicine> {
        return dao.getAlarmsToRescheduleEveryMonth(medicineName, alarmsPerDay)
    }

    override suspend fun deleteUpcomingAlarms(medicineName: String, currentTimeMillis: Long) {
        return dao.deleteUpcomingAlarms(medicineName, currentTimeMillis)
    }

    override fun getLastAlarmFromAllDistinctMedicines(): List<Medicine> {
        return dao.getLastAlarmFromAllDistinctMedicines()
    }

    override suspend fun updateMedicinesActiveStatus(
        medicineName: String,
        currentTimeMillis: Long,
        isActive: Boolean
    ) {
        return dao.updateMedicinesActiveStatus(medicineName, currentTimeMillis, isActive)
    }

    override suspend fun getAlarmTimesForMedicine(medicineName: String, cutoffTime: Long, treatmentID: String): List<String>{
        return dao.getAlarmTimesForMedicine(medicineName, cutoffTime, treatmentID)
    }
}