package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MedicinesViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
): ViewModel() {

    val medicines = medicineRepository.getAllMedicines()

    suspend fun getMedicines(): List<Medicine>{
        return withContext(Dispatchers.IO){
            medicineRepository.getMedicines()
        }
    }

    fun getCurrentAlarmData(alarmInMillis: Long): Medicine? {
        return medicineRepository.getCurrentAlarmData(alarmInMillis)
    }

    suspend fun hasNextAlarmData(medicineName: String, currentTimeMillis: Long): Boolean{
        return withContext(Dispatchers.IO){
            medicineRepository.hasNextAlarmData(medicineName, currentTimeMillis)
        }
    }

    suspend fun getNextAlarmData(medicineName: String, currentTimeMillis: Long): Medicine? {
        return withContext(Dispatchers.IO){
            medicineRepository.getNextAlarmData(medicineName, currentTimeMillis)
        }
    }

    suspend fun getFirstMedicineOfNextDay(nextDayInMillis: Long): Medicine?{
        return withContext(Dispatchers.IO){
            medicineRepository.getFirstMedicineOfNextDay(nextDayInMillis)
        }
    }

    suspend fun getFirstMedicineOfTheDay(millis: Long): Medicine?{
        return withContext(Dispatchers.IO){
            medicineRepository.getFirstMedicineOfTheDay(millis)
        }
    }

    fun getWorkerID(medicineName: String): String {
        return medicineRepository.getWorkerID(medicineName)
    }

    fun getAllDistinctMedicines(): List<Medicine> {
        return medicineRepository.getAllDistinctMedicines()
    }

    fun getAllMedicinesWithSameName(medicineName: String): List<Medicine> {
        return medicineRepository.getAllMedicinesWithSameName(medicineName)
    }

    fun insertMedicines(medicine: List<Medicine>) = viewModelScope.launch{
        withContext(Dispatchers.IO){
            medicineRepository.insertMedicines(medicine)
        }
    }

    fun updateMedicines(medicine: Medicine) = viewModelScope.launch{
        withContext(Dispatchers.IO){
            medicineRepository.updateMedicine(medicine)
        }
    }

    fun deleteMedicines(medicine: Medicine) = viewModelScope.launch{
        withContext(Dispatchers.IO){
            medicineRepository.deleteMedicine(medicine)
        }
    }

    fun deleteAllSelectedMedicines(medicines: List<Medicine>) = viewModelScope.launch{
        withContext(Dispatchers.IO){
            medicineRepository.deleteAllSelectedMedicines(medicines)
        }
    }

    fun removeRemainingAlarms(medicine: Medicine) = viewModelScope.launch {
        withContext(Dispatchers.IO){
            val medicinesToDelete = medicineRepository.getAlarmsAfterProvidedMillis(medicine.name, System.currentTimeMillis())
            medicineRepository.deleteAllSelectedMedicines(medicinesToDelete)
        }
    }

    suspend fun getAlarmTimesForMedicine(medicineName: String, cutoffTime: Long): List<String> {
        return medicineRepository.getAlarmTimesForMedicine(medicineName, cutoffTime)
    }

    suspend fun getAlarmsAfterProvidedMillis(medicineName: String, millis: Long): List<Medicine>{
        return withContext(Dispatchers.IO){
            medicineRepository.getAlarmsAfterProvidedMillis(medicineName, millis)
        }
    }

    suspend fun getAlarmTimeSinceMidnight(medicineName: String): Long {
        return withContext(Dispatchers.IO){
            medicineRepository.getAlarmTimeSinceMidnight(medicineName)
        }
    }

    suspend fun getMedicineEditTimestamp(medicineName: String): Long{
        return withContext(Dispatchers.IO){
            medicineRepository.getMedicineEditTimestamp(medicineName)
        }
    }

    suspend fun getMillisList(medicineName: String, alarmsPerDay: Int): List<Long>{
        return withContext(Dispatchers.IO){
            medicineRepository.getDailyAlarms(medicineName, alarmsPerDay)
        }
    }

    suspend fun endTreatment(medicine: Medicine) = viewModelScope.launch {
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        alarmScheduler.cancelAlarm(alarmItem, true)

        withContext(Dispatchers.IO){
            medicineRepository.updateMedicinesActiveStatus(medicine.name, System.currentTimeMillis(), false)
            medicineRepository.deleteUpcomingAlarms(medicine.name, System.currentTimeMillis())
        }
    }

}