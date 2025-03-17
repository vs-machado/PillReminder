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

    /**
     * Checks if this is the first time the app is being run
     */
    fun isFirstRun(): Boolean

    /**
     * Marks that the app's first run setup is complete
     */
    suspend fun setFirstRunComplete()

    /**
     * Sets the standard notification sound to be used for alarms.
     * Currently has 2 different alarms.
     */
    fun setAlarmSound(uri: String)

    /**
     * Gets the URI of the standard notification sound to be used for alarms.
     */
    fun getAlarmSound(): String

    /**
     * Retrieves the notification channel id.
     * UUID are used because app allows users to change the notification sound in settings.
     * For changing the notification sounds, it is necessary to delete the old channel and
     * create a new one with an unique identifier.
     * It uses a prefix that defines if the channel is for medicine alarms, pillbox reminders or follow up alarm.
     * For instance: "AlarmChannel-37108bbf-3f45-48b9-a40c-b97561c0e541"
     *                                  or
     *               "FollowUpAlarmChannel-37108bbf-3f45-48b9-a40c-b97561c0e541"
     *
     * @return String that contains the notification channel identifier
     */
    fun getChannelId(): String

    /**
     * Sets the notification channel id with the pattern defined on [getChannelId].
     */
    fun setChannelId(channelId: String)
}