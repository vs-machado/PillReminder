package com.phoenix.pillreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDao
import kotlinx.coroutines.launch

class MedicinesViewModel(private val dao: MedicineDao): ViewModel() {

    val medicines = dao.getAllMedicines()

    fun insertMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.insertMedicine(medicine)
    }

    fun updateMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.updateMedicine(medicine)
    }

    fun deleteMedicines(medicine: Medicine) = viewModelScope.launch{
        dao.deleteMedicine(medicine)
    }
}