package com.phoenix.pillreminder.feature_alarms.presentation

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class OnOneOffFabClickListener: View.OnClickListener {
    companion object {
        private const val MIN_CLICK_INTERVAL = 600L
        private var isViewClicked = false
    }

    private var mLastClickTime: Long = 0

    abstract fun onSingleClick(fab: FloatingActionButton)

    override fun onClick(v: View) {
        val currentClickTime = SystemClock.uptimeMillis()
        val elapsedTime = currentClickTime - mLastClickTime
        mLastClickTime = currentClickTime

        if (elapsedTime <= MIN_CLICK_INTERVAL) return
        if (!isViewClicked) {
            isViewClicked = true
            startTimer()
        } else {
            return
        }
        onSingleClick(v as FloatingActionButton)
    }

    private fun startTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            isViewClicked = false
        }, MIN_CLICK_INTERVAL)
    }
}
