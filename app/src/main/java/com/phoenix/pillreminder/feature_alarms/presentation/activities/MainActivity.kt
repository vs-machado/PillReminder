package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityMainBinding
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.utils.languageMapping
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
    @Inject
    lateinit var repository: MedicineRepository

    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        checkAndShowTutorial()
        setAppLanguagePreference()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.appBarMain.toolbarHome)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupNavigation(){
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView3) as NavHostFragment
        val navController = navHostFragment.navController
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_out)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.fade_out)
            .build()

        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.bottom_home -> {
                    navController.navigate(R.id.homeFragment, null, navOptions)
                    true
                }
                R.id.bottom_medicines -> {
                    navController.navigate(R.id.myMedicinesFragment, null, navOptions)
                    true
                }
                R.id.bottom_help -> {
                    navController.navigate(R.id.helpFragment, null, navOptions)
                    true
                }
                R.id.settings -> {
                    navController.navigate(R.id.mySettingsFragment, null, navOptions)
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
                    R.id.mySettingsFragment -> R.id.settings
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

    private fun setAppLanguagePreference() {
        val phoneLanguage = Locale.getDefault().toLanguageTag()
        val language = languageMapping[phoneLanguage] ?: "en"

        lifecycleScope.launch(Dispatchers.IO){
            sharedPreferencesRepository.setAppLanguage(language)
        }
    }

//    override fun attachBaseContext(newBase: Context?) {
//        val languageCode = sharedPreferencesRepository.getAppLanguage()
//
//        if(languageCode != null && newBase != null){
//            val context: Context = LanguageConfig.changeLanguage(newBase, languageCode)
//            super.attachBaseContext(context)
//            return
//        }
//
//        super.attachBaseContext(newBase)
//    }
}