package com.phoenix.pillreminder.feature_alarms.presentation.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.data.ads.GoogleMobileAdsConsentManager
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.PermissionManager
import com.phoenix.pillreminder.feature_alarms.presentation.utils.LanguageConfig
import com.phoenix.pillreminder.feature_alarms.presentation.utils.ThemeUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// User can change the app language, disable battery optimizations and request app permissions
@AndroidEntryPoint
class MySettingsFragment: PreferenceFragmentCompat() {

    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(requireActivity())

        // Only show the consent options preference if user lives on EU or in certain states of US.
        if(googleMobileAdsConsentManager.isPrivacyOptionsRequired){
            findPreference<PreferenceCategory>("header_privacy")?.isVisible = true
            findPreference<Preference>("consent_options")?.isVisible = true
        }

        setupPreferenceListeners()
    }

    // Overrides recyclerview properties to add a 80dp bottom margin, avoiding settings overlapping with
    // the bottom navigation bar
    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        val marginBottom = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            80f,
            resources.displayMetrics
        ).toInt()

        (recyclerView.layoutParams as ViewGroup.MarginLayoutParams).let { params ->
            params.bottomMargin = marginBottom
            recyclerView.layoutParams = params
        }

        return recyclerView
    }

    override fun onResume() {
        super.onResume()

        // Apply theme
        ThemeUtils.applyThemeBasedSystemColors(
            requireActivity(),
            R.color.white_ice,
            R.color.white_ice,
            R.color.dark_gray,
            R.color.dark_gray,
            isAppearanceLightStatusBar = true,
            isAppearanceLightNavigationBar = true,
            isAppearanceLightStatusBarNightMode = false,
            isAppearanceLightNavigationBarNightMode = false
        )

        // Avoid FAB recreation during screen rotation
        requireActivity().findViewById<FloatingActionButton>(R.id.fabAddMedicine).visibility = View.GONE
    }

    private fun setupPreferenceListeners() {
        val languagePreference = findPreference<ListPreference>("language")
        val appLanguage = sharedPreferencesRepository.getAppLanguage()

        val snoozeIntervalPreference = findPreference<ListPreference>("snooze_interval")

        languagePreference?.apply {
            value = appLanguage
            setOnPreferenceChangeListener { _, newValue ->
                sharedPreferencesRepository.setAppLanguage(newValue as String)
                LanguageConfig.changeLanguage(newValue)
                true
            }
        }

        // Permissions preference
        findPreference<Preference>("permissions")
            ?.setOnPreferenceClickListener {
                requestAppPermissions()
                true
            }

        // Disabling battery optimizations ensure that notifications will be delivered on time
        findPreference<Preference>("battery_optimizations")
            ?.setOnPreferenceClickListener {
                disableBatteryOptimizations()
                true
            }

        // Allow users to change the alarm snooze interval
        snoozeIntervalPreference?.apply {
            value = sharedPreferencesRepository.getSnoozeInterval().toString()
            setOnPreferenceChangeListener { _, newValue ->
                sharedPreferencesRepository.setSnoozeInterval(newValue.toString().toInt())
                true
            }
        }

        // Allows the users to change their data usage consent
        if(googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
            findPreference<Preference>("consent_options")
                ?.setOnPreferenceClickListener {
                    googleMobileAdsConsentManager.showPrivacyOptionsForm(requireActivity()) { formError ->
                        if(formError != null) {
                            Log.d("MySettingsFragment", formError.message.toString())
                        }
                    }
                    true
                }
        }
    }

    // Request POST_NOTIFICATIONS permission and overlay permission
    private fun requestAppPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if(!Settings.canDrawOverlays(context)){
            requestOverlayPermissionLauncher.launch(PermissionManager.getOverlayPermissionIntent(requireContext()))
        }
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(requireContext())){
            Toast.makeText(requireContext(), getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        requestOverlayPermissionLauncher.launch(PermissionManager.getOverlayPermissionIntent(requireContext()))
    }

    private val requestOverlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private fun disableBatteryOptimizations() {
        val packageName = requireContext().packageName
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager

        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            // Already ignoring battery optimizations
            Toast.makeText(context, getString(R.string.optimizations_already_disabled), Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Fallback to general battery settings if the specific action is not available
                val fallbackIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                startActivity(fallbackIntent)
            }
        }
    }
}