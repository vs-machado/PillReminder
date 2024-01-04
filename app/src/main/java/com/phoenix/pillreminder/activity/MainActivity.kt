package com.phoenix.pillreminder.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.phoenix.pillreminder.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.navigation_toolbar)
        setSupportActionBar(toolbar)
    }
}