package com.phoenix.remedi.feature_alarms.data.repository

import android.content.Context
import com.phoenix.remedi.feature_alarms.domain.repository.SharedPreferencesRepository
import java.util.Locale

class SharedPreferencesRepositoryImpl(
    private val context: Context
): SharedPreferencesRepository {

    private val sharedPreferencesPermissionRequest = context.getSharedPreferences("dont_show_again", Context.MODE_PRIVATE)
    private val sharedPreferencesPillbox = context.getSharedPreferences("pillbox_reminder", Context.MODE_PRIVATE)
    private val sharedPreferencesAlarmReschedule = context.getSharedPreferences("alarm_reschedule", Context.MODE_PRIVATE)
    private val sharedPreferencesLanguage = context.getSharedPreferences("language", Context.MODE_PRIVATE)
    private val sharedPreferencesSnoozeInterval = context.getSharedPreferences("snooze_interval", Context.MODE_PRIVATE)
    private val sharedPreferencesPillboxReminderHour = context.getSharedPreferences("pillbox_reminder_hour", Context.MODE_PRIVATE)
    private val sharedPreferencesFirstRun = context.getSharedPreferences("first_run", Context.MODE_PRIVATE)

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
        sharedPreferencesAlarmReschedule.edit().putBoolean("alarms_rescheduled", boolean).apply()
    }

    override fun getAlarmReschedulePreferences(): Boolean {
        return sharedPreferencesAlarmReschedule.getBoolean("alarms_rescheduled", false)
    }

    override fun getAppLanguage(): String? {
        return sharedPreferencesLanguage.getString("language", Locale.getDefault().language.toString())
    }

    override fun setAppLanguage(language: String) {
        sharedPreferencesLanguage.edit().putString("language", language).apply()
    }

    override fun setSnoozeInterval(minutes: Int) {
        sharedPreferencesSnoozeInterval.edit().putInt("snooze_interval", minutes).apply()
    }

    override fun getSnoozeInterval(): Int {
        return sharedPreferencesSnoozeInterval.getInt("snooze_interval", 5)
    }

    override fun setPillboxReminderHour(hours: Int, minutes: Int) {
        sharedPreferencesPillboxReminderHour.edit().putInt("pillbox_hour", hours).putInt("pillbox_minute", minutes).apply()
    }

    override fun getPillboxReminderHour(): Pair<Int?, Int?> {
        val hour = sharedPreferencesPillboxReminderHour.getInt("pillbox_hour", -1)
        val minute = sharedPreferencesPillboxReminderHour.getInt("pillbox_minute", -1)
        
        return Pair(
            if (hour == -1) null else hour,
            if (minute == -1) null else minute
        )
    }

    override fun isFirstRun(): Boolean {
        return !sharedPreferencesFirstRun.contains("first_run_complete")
    }

    override suspend fun setFirstRunComplete() {
        sharedPreferencesFirstRun.edit().putBoolean("first_run_complete", true).apply()
    }
}