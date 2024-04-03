package com.phoenix.pillreminder.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicinesViewModel(private val dao: MedicineDao): ViewModel() {

    val medicines = dao.getAllMedicines()

    suspend fun getMedicines(): List<Medicine>{
        return withContext(Dispatchers.IO){
            dao.getMedicines()
        }
    }

    fun getCurrentAlarmData(alarmInMillis: Long): Medicine? {
        return dao.getCurrentAlarmData(alarmInMillis)
    }

    fun getAllMedicinesWithSameName(medicineName: String): List<Medicine> {
        return dao.getAllMedicinesWithSameName(medicineName)
    }

    fun insertMedicines(medicine: List<Medicine>) = viewModelScope.launch{
        dao.insertMedicines(medicine)
    }

    fun updateMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.updateMedicine(medicine)
    }

    fun deleteMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.deleteMedicine(medicine)
    }

    fun deleteAllSelectedMedicines(medicines: List<Medicine>) = viewModelScope.launch{
        dao.deleteAllSelectedMedicines(medicines)
    }



}