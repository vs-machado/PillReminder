package com.phoenix.pillreminder.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDao
import kotlinx.coroutines.launch

class MedicinesViewModel(private val dao: MedicineDao): ViewModel() {

    val medicines = dao.getAllMedicines()

    fun insertMedicines(medicine: List<Medicine>) = viewModelScope.launch{
        dao.insertMedicines(medicine)
    }

    fun updateMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.updateMedicine(medicine)
    }

    fun deleteMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.deleteMedicine(medicine)
    }
}