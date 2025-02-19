package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity


object ThemeUtils {

    // Needs to implement navigation bar color change. It's currently not being applied. The navigation bar is transparent.
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val insetsController = WindowInsetsControllerCompat(activity.window, activity.window.decorView)

            // Calculate the status bar height and then sets it's color.
            activity.window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                val currentNightMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)

                when(currentNightMode) {
                    Configuration.UI_MODE_NIGHT_NO -> {
                        BarUtils.applyStatusBarColor(activity.window, ContextCompat.getColor(activity, statusBarColorLightMode), true, statusBarInsets.top)
                        insetsController.isAppearanceLightStatusBars = isAppearanceLightStatusBar
                    }
                    Configuration.UI_MODE_NIGHT_YES -> {
                        BarUtils.applyStatusBarColor(activity.window, ContextCompat.getColor(activity, statusBarColorNightMode), true, statusBarInsets.top)
                        insetsController.isAppearanceLightStatusBars = isAppearanceLightStatusBarNightMode
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                        BarUtils.applyStatusBarColor(activity.window, ContextCompat.getColor(activity, statusBarColorLightMode), true, statusBarInsets.top)
                        insetsController.isAppearanceLightStatusBars = isAppearanceLightStatusBar
                    }
                }
                insets
            }
        } else {
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
}