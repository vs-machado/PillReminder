package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDao
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.data.repository.MedicineRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.MedicinesViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var dao: MedicineDao
    lateinit var factory: MedicinesViewModelFactory
    private lateinit var repository: MedicineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dao = MedicineDatabase.getInstance(this).medicineDao()
        repository = MedicineRepositoryImpl(dao)
        factory = MedicinesViewModelFactory(repository)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    }
}