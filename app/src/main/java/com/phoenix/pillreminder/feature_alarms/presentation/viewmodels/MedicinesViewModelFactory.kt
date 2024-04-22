package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import java.lang.IllegalArgumentException

class MedicinesViewModelFactory(
    private val medicineRepository: MedicineRepository
) :ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MedicinesViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return MedicinesViewModel(medicineRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}