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
}