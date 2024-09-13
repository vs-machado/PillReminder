package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityMainBinding
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.fragments.HelpFragment
import com.phoenix.pillreminder.feature_alarms.presentation.fragments.HomeFragment
import com.phoenix.pillreminder.feature_alarms.presentation.fragments.MyMedicinesFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
    @Inject
    lateinit var repository: MedicineRepository

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // disables night mode
        checkAndShowTutorial()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.appBarMain.toolbarHome)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupNavigation(){
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView3) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.bottom_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.bottom_medicines -> {
                    navController.navigate(R.id.myMedicinesFragment)
                    true
                }
                R.id.bottom_help -> {
                    navController.navigate(R.id.helpFragment)
                    true
                }
                else -> return@setOnItemSelectedListener false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.menu.findItem(
                when (destination.id) {
                    R.id.homeFragment -> R.id.bottom_home
                    R.id.myMedicinesFragment -> R.id.bottom_medicines
                    R.id.helpFragment -> R.id.bottom_help
                    else -> return@addOnDestinationChangedListener
                }
            ).isChecked = true
        }
    }

    private fun checkAndShowTutorial(){
        if(!isTutorialShown()){
            startActivity(Intent(this, MyAppIntro::class.java))
            markTutorialAsShown()
        }
    }

    private fun isTutorialShown(): Boolean {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getBoolean("isTutorialShown", false)
    }

     private fun markTutorialAsShown(){
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isTutorialShown", true)
        editor.apply()
    }
}