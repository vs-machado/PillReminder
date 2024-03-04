package com.phoenix.pillreminder.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.db.MedicineDao
import com.phoenix.pillreminder.db.MedicineDatabase
import com.phoenix.pillreminder.model.MedicinesViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var dao: MedicineDao
    lateinit var factory: MedicinesViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dao = MedicineDatabase.getInstance(this).medicineDao()
        factory = MedicinesViewModelFactory(dao)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    }
}