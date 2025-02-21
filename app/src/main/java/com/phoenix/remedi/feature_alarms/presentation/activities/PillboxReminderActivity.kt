package com.phoenix.remedi.feature_alarms.presentation.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.phoenix.remedi.R
import com.phoenix.remedi.databinding.ActivityPillboxReminderBinding
import com.phoenix.remedi.feature_alarms.presentation.AlarmService

/**
 * Responsible for displaying a pillbox refill reminder to the user.
 */
class PillboxReminderActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPillboxReminderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPillboxReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNotificationAndStatusBar()

        // Clicking on dismiss button will navigate the user to the main activity and dismiss the notification.
        binding.btnDismissPillbox.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java)

            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(999)
            val stopServiceIntent = Intent(this, AlarmService::class.java)
            this.stopService(stopServiceIntent)

            startActivity(intent)
            finish()
        }
    }

    private fun setupNotificationAndStatusBar(){
        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            window.statusBarColor = resources.getColor(R.color.blue_gradient_start, null)
            window.navigationBarColor = resources.getColor(R.color.blue_gradient_end, null)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        } else {
            window.statusBarColor = resources.getColor(R.color.blue_gradient_start, null)
            window.navigationBarColor = resources.getColor(R.color.white, null)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true
        }

    }
}