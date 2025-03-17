package com.phoenix.remedi.feature_alarms.presentation.fragments

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
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
import com.phoenix.remedi.R
import com.phoenix.remedi.feature_alarms.data.ads.GoogleMobileAdsConsentManager
import com.phoenix.remedi.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.remedi.feature_alarms.presentation.PermissionManager
import com.phoenix.remedi.feature_alarms.presentation.utils.LanguageConfig
import com.phoenix.remedi.feature_alarms.presentation.utils.ThemeUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

/**
 * Settings fragment that allows users to:
 * - Change the application language
 * - Manage notification and overlay permissions
 * - Disable battery optimizations for reliable notifications
 * - Configure notification sounds
 * - Adjust snooze interval settings
 * - Manage privacy consent options (for EU/US users)
 */
@AndroidEntryPoint
class MySettingsFragment: PreferenceFragmentCompat() {

    @Inject lateinit var sharedPreferencesRepository: SharedPreferencesRepository
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(requireActivity())

        // Only show the consent options preference if user lives on EU or in certain states of US.
        if(googleMobileAdsConsentManager.isPrivacyOptionsRequired){
            findPreference<PreferenceCategory>("header_privacy")?.isVisible = true
            findPreference<Preference>("consent_options")?.isVisible = true
        }

        // Language change disabled on Android 12 due to a bug
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            findPreference<ListPreference>("language")?.isVisible = true
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

        // Changes notification sound
        findPreference<ListPreference>("notification_sound")?.setOnPreferenceChangeListener { _, newValue ->
            val notificationManager = activity?.getSystemService(NotificationManager::class.java)
            val channelId = sharedPreferencesRepository.getChannelId()

            // Users can listen the selected option sound when clicking on an option
            playNotificationSound(newValue as String)

            // Notification channel needs to be destroyed for updating the notification sound
            notificationManager?.let {
                notificationManager.deleteNotificationChannel("AlarmChannel-$channelId")
                notificationManager.deleteNotificationChannel("FollowUpAlarmChannel-$channelId")
                notificationManager.deleteNotificationChannel("PillboxReminderChannel-$channelId")
            }

            // Generates a new channelId for a channel to be instanced in NotificationUtils
            sharedPreferencesRepository.setChannelId(UUID.randomUUID().toString())
            sharedPreferencesRepository.setAlarmSound(newValue as String)
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

    private fun playNotificationSound(option: String) {
        mediaPlayer?.release()

        // Check if device is muted and warn the user
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (currentVolume == 0) {
            Toast.makeText(requireContext(), getString(R.string.turn_up_volume), Toast.LENGTH_LONG).show()
        }

        val soundResId = when(option) {
            "option1"  -> R.raw.alarm_2
            "option2" -> R.raw.alarm_sound
            else -> R.raw.alarm_2
        }

        mediaPlayer = MediaPlayer.create(requireContext(), soundResId).apply{
            setOnCompletionListener { release() }
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}