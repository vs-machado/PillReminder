package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicinesViewModel(private val medicineRepository: MedicineRepository): ViewModel() {

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

    fun getWorkerID(medicineName: String): String {
        return medicineRepository.getWorkerID(medicineName)
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



}