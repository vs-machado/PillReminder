package com.phoenix.pillreminder.feature_alarms.presentation.utils

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.phoenix.pillreminder.R

object BarUtils {
    private const val TAG_STATUS_BAR = "TAG_STATUS_BAR"

    // for Lollipop (SDK 21+)
    fun transparentStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val option =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        val vis = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = option or vis
        window.statusBarColor = Color.TRANSPARENT
    }

    // for the latest SDK (24+)

    fun applyStatusBarColor (window: Window, color: Int, isDecor: Boolean, getStatusBarHeight: Int): View {
        val parent = if (isDecor) window.decorView as ViewGroup else (window.findViewById<View>(android.R.id.content) as ViewGroup)
        var fakeStatusBarView = parent.findViewWithTag<View>(TAG_STATUS_BAR)
        if (fakeStatusBarView != null) {
            if (fakeStatusBarView.visibility == View.GONE) {
                fakeStatusBarView.visibility = View.VISIBLE
            }
            fakeStatusBarView.setBackgroundColor(color)
        } else {
            fakeStatusBarView = createStatusBarView(window, color, getStatusBarHeight)
            parent.addView(fakeStatusBarView)
        }
        return fakeStatusBarView
    }

    private fun createStatusBarView (window: Window, color: Int, getStatusBarHeight: Int): View {
        val statusBarView = View(window.context)
        statusBarView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight
        )
        statusBarView.setBackgroundColor(color)
        statusBarView.tag = TAG_STATUS_BAR
        return statusBarView
    }
}