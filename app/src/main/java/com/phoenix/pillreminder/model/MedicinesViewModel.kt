package com.phoenix.pillreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.db.Medicines
import com.phoenix.pillreminder.db.MedicinesDao
import kotlinx.coroutines.launch

class MedicinesViewModel(private val dao: MedicinesDao): ViewModel() {

    val medicines = dao.getAllMedicines()

    fun insertMedicines(medicine: Medicines) = viewModelScope.launch{
        dao.insertMedicine(medicine)
    }

    fun updateMedicines(medicine: Medicines) = viewModelScope.launch{
        dao.updateMedicine(medicine)
    }

    fun deleteMedicines(medicine: Medicines) = viewModelScope.launch{
        dao.deleteMedicine(medicine)
    }
}