package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyMedicinesViewModel @Inject constructor(
    private val repository: MedicineRepository
): ViewModel() {
}