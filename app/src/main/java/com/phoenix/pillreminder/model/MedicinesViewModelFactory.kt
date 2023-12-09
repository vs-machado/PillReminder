package com.phoenix.pillreminder.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.phoenix.pillreminder.MedicinesViewModel
import com.phoenix.pillreminder.db.MedicineDao
import java.lang.IllegalArgumentException

class MedicinesViewModelFactory(
    private val dao: MedicineDao
) :ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MedicinesViewModel::class.java)){
            return MedicinesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}