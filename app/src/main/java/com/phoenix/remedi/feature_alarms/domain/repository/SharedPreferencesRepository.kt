package com.phoenix.remedi.feature_alarms.domain.repository

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

    /**
     *  Stores the pillbox reminder hour.
     */
    fun setPillboxReminderHour(hours: Int, minutes: Int)

    /**
     *  Returns the previously stored pillbox reminder hour.
     *  The stored hour is used to reschedule the reminders if user restarts the device or
     *  updates the app.
     *
     *  If user does not set the pillbox reminder hour, it defaults to null
     */
    fun getPillboxReminderHour(): Pair<Int?, Int?>
}