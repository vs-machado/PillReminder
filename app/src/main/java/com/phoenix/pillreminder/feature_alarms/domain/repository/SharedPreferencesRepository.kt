package com.phoenix.pillreminder.feature_alarms.domain.repository

interface SharedPreferencesRepository {

    fun setPermissionRequestPreferences(boolean: Boolean)

    fun setPillboxPreferences(boolean: Boolean)

    fun getPermissionRequestPreferences(): Boolean

    fun getPillboxPreferences(): Boolean

    fun setAlarmReschedulePreferences(boolean: Boolean)

    fun getAlarmReschedulePreferences(): Boolean

    fun getAppLanguage(): String?

    fun setAppLanguage(language: String)

    /**
     *   Store the snooze interval selected by the user in app settings
     *
     *   @param minutes the snooze interval in minutes
     */
    fun setSnoozeInterval(minutes: Int)

    /**
     * Returns the snooze interval in minutes that was previously set by the user.
     */
    fun getSnoozeInterval(): Int
}