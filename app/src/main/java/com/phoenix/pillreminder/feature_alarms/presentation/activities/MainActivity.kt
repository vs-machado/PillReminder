package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityMainBinding
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
    @Inject
    lateinit var repository: MedicineRepository

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbarHome)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView3) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment
            ), binding.dlMainActivity
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navigationView.setNavigationItemSelectedListener {menuItem ->
            when(menuItem.itemId){
                R.id.home_item -> {
                    navController.navigate(R.id.homeFragment)
                }
                R.id.mymedicines_item -> {
                   navController.navigate(R.id.myMedicinesFragment)
                }
                R.id.help_item -> {
                    navController.navigate(R.id.helpFragment)
                }
                R.id.about_item -> {
                    navController.navigate(R.id.aboutFragment)
                }
            }

            menuItem.isChecked = true
            binding.dlMainActivity.close()
            true

        }

        navController.addOnDestinationChangedListener{ _, destination, _ ->
            when(destination.id){
                R.id.homeFragment -> binding.navigationView.setCheckedItem(R.id.home_item)
                R.id.myMedicinesFragment -> binding.navigationView.setCheckedItem(R.id.mymedicines_item)
                R.id.helpFragment -> binding.navigationView.setCheckedItem(R.id.help_item)
                R.id.aboutFragment -> binding.navigationView.setCheckedItem(R.id.about_item)
            }
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if(!isTutorialShown()){
            startActivity(Intent(this, MyAppIntro::class.java))
            markTutorialAsShown()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView3) as NavHostFragment
        val navController = navHostFragment.navController

        return when {
            binding.dlMainActivity.isDrawerOpen(GravityCompat.START) -> {
                binding.dlMainActivity.closeDrawer(GravityCompat.START)
                true
            }
            navController.currentDestination?.id == R.id.homeFragment -> {
                binding.dlMainActivity.openDrawer(GravityCompat.START)
                true
            }
            else -> navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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