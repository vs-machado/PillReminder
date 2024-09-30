package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.ExpiredMedicinesInfo
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

    private val _lastAlarm = MutableLiveData<Medicine>()
    val lastAlarm: LiveData<Medicine>
        get() = _lastAlarm

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

    fun getWorkerID(medicineName: String, treatmentID: String): String {
        return medicineRepository.getWorkerID(medicineName, treatmentID)
    }

    fun getLastAlarmFromAllDistinctMedicines(): List<Medicine> {
        return medicineRepository.getLastAlarmFromAllDistinctMedicines()
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

    suspend fun updateExpiredMedicines(info: ExpiredMedicinesInfo) = viewModelScope.launch {
        withContext(Dispatchers.IO){
            medicineRepository.updateExpiredMedicines(
                info.treatmentID,
                info.name,
                info.quantity,
                info.form,
                info.endDate,
                info.frequency,
                info.currentTime
            )
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

    suspend fun getSelectedDaysList(medicineName: String, treatmentID: String): MutableSet<Int> {
        return withContext(Dispatchers.IO) {
            Log.d("debug", "$medicineName $treatmentID")
            val daysString = medicineRepository.getSelectedDaysList(medicineName, treatmentID)
            Log.d("debug", daysString)
            daysString.split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
        }
    }
    fun removeRemainingAlarms(medicine: Medicine) = viewModelScope.launch {
        withContext(Dispatchers.IO){
            val medicinesToDelete = medicineRepository.getAlarmsAfterProvidedMillis(medicine.name, System.currentTimeMillis())
            medicineRepository.deleteAllSelectedMedicines(medicinesToDelete)
        }
    }

    suspend fun getAlarmTimesForMedicine(medicineName: String, cutoffTime: Long, treatmentID: String): List<String> {
        return medicineRepository.getAlarmTimesForMedicine(medicineName, cutoffTime, treatmentID)
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

    suspend fun getDailyAlarms(medicineName: String, alarmsPerDay: Int, treatmentID: String): List<Pair<Int, Int>>{
        return withContext(Dispatchers.IO){
            medicineRepository.getDailyAlarms(medicineName, alarmsPerDay, treatmentID).map {
                Pair(it.hour, it.minute)
            }
        }
    }

    suspend fun endTreatment(medicine: Medicine) = viewModelScope.launch {
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

        alarmScheduler.cancelAlarm(alarmItem, true)

        withContext(Dispatchers.IO){
            medicineRepository.updateMedicinesActiveStatus(medicine.name, System.currentTimeMillis(), false)
            medicineRepository.deleteUpcomingAlarms(medicine.name, System.currentTimeMillis())
        }
    }

    suspend fun getLastAlarm(medicineName: String, treatmentID: String): Medicine {
        return withContext(Dispatchers.IO){
            medicineRepository.getLastAlarm(medicineName, treatmentID)
        }
    }

}