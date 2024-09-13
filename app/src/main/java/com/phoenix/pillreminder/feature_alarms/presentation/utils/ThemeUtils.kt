package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.content.res.Configuration
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity

object ThemeUtils {

    fun applyThemeBasedSystemColors(
        activity: FragmentActivity,
        statusBarColorLightMode: Int,
        navigationBarColorLightMode: Int,
        statusBarColorNightMode: Int,
        navigationBarColorNightMode: Int,
        isAppearanceLightStatusBar: Boolean,
        isAppearanceLightNavigationBar: Boolean,
        isAppearanceLightStatusBarNightMode: Boolean,
        isAppearanceLightNavigationBarNightMode: Boolean
    ) {
        // Get the current night mode
        val currentNightMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set status bar and navigation bar colors based on the theme
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                // Light theme
                activity.window.statusBarColor = activity.resources.getColor(statusBarColorLightMode, null)
                activity.window.navigationBarColor = activity.resources.getColor(navigationBarColorLightMode, null)
                WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = isAppearanceLightStatusBar
                    isAppearanceLightNavigationBars = isAppearanceLightNavigationBar
                }
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                // Dark theme
                activity.window.statusBarColor = activity.resources.getColor(statusBarColorNightMode, null)
                activity.window.navigationBarColor = activity.resources.getColor(navigationBarColorNightMode, null)
                WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = isAppearanceLightStatusBarNightMode
                    isAppearanceLightNavigationBars = isAppearanceLightNavigationBarNightMode
                }
            }
        }
    }
}