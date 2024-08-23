package com.phoenix.pillreminder.feature_alarms.data.repository

import android.content.Context
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository

class SharedPreferencesRepositoryImpl(
    private val context: Context
): SharedPreferencesRepository {

    private val sharedPreferencesPermissionRequest = context.getSharedPreferences("dont_show_again", Context.MODE_PRIVATE)
    private val sharedPreferencesPillbox = context.getSharedPreferences("pillbox_reminder", Context.MODE_PRIVATE)
    private val sharedPreferencesAlarmReschedule = context.getSharedPreferences("alarm_reschedule", Context.MODE_PRIVATE)

    override fun setPermissionRequestPreferences(boolean: Boolean) {
        sharedPreferencesPermissionRequest.edit().putBoolean("dont_show_again", boolean).apply()
    }

    override fun setPillboxPreferences(boolean: Boolean) {
        sharedPreferencesPillbox.edit().putBoolean("pillbox_reminder", boolean).apply()
    }

    override fun getPermissionRequestPreferences(): Boolean {
        return sharedPreferencesPermissionRequest.getBoolean("dont_show_again", false)
    }

    override fun getPillboxPreferences(): Boolean {
        return sharedPreferencesPillbox.getBoolean("pillbox_reminder", false)
    }

    override fun setAlarmReschedulePreferences(boolean: Boolean) {
        sharedPreferencesAlarmReschedule.edit().putBoolean("alarms_reschedule", boolean).apply()
    }

    override fun getAlarmReschedulePreferences(): Boolean {
        return sharedPreferencesAlarmReschedule.getBoolean("alarms_rescheduled", false)
    }
}