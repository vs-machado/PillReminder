package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.databinding.ActivityMainBinding
import com.phoenix.pillreminder.feature_alarms.data.ads.Admob
import com.phoenix.pillreminder.feature_alarms.data.ads.Admob.loadAd
import com.phoenix.pillreminder.feature_alarms.data.ads.GoogleMobileAdsConsentManager
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.utils.NotificationUtils
import com.phoenix.pillreminder.feature_alarms.presentation.utils.languageMapping
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Admob setup
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        checkAndShowTutorial()
        setAppLanguagePreference()

        val initializeAds: () -> Unit = {
            CoroutineScope(Dispatchers.IO).launch {
                // Initialize the Google Mobile Ads SDK on a background thread.
                MobileAds.initialize(this@MainActivity) {}
                runOnUiThread {
                    // Load an ad on the main thread.
                    loadAd(this@MainActivity)
                }
            }
        }

        Admob.gatherUserConsent(this, initializeAds)

        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val menuItemView = findViewById<View>(item.itemId)
        val activity = this
        PopupMenu(this, menuItemView).apply {
            menuInflater.inflate(R.menu.popup_menu, menu)
            menu
                .findItem(R.id.privacy_settings)
                .setVisible(googleMobileAdsConsentManager.isPrivacyOptionsRequired)
            show()
            setOnMenuItemClickListener { popupMenuItem ->
                when (popupMenuItem.itemId) {
                    R.id.privacy_settings -> {
                        // Handle changes to user consent.
                        googleMobileAdsConsentManager.showPrivacyOptionsForm(activity) { formError ->
                            if (formError != null) {
                                Toast.makeText(activity, formError.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        true
                    }
                    R.id.ad_inspector -> {
                        MobileAds.openAdInspector(activity) { error ->
                            // Error will be non-null if ad inspector closed due to an error.
                            error?.let { Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show() }
                        }
                        true
                    }
                    // Handle other branches here.
                    else -> false
                }
            }
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // If user receives a pillbox reminder notification and click on it,
        // the activity opens and dismisses the notification.
        NotificationUtils.dismissNotification(context = this)
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

    companion object {
        private const val TAG = "MainActivity"

        // Check your logcat output for the test device hashed ID e.g.
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
        // to get test ads on this device" or
        // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
        // a debug device".
        const val TEST_DEVICE_HASHED_ID = "ABCDEF012345"
    }
}