package com.phoenix.pillreminder.alarmscheduler

import android.content.Context
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.phoenix.pillreminder.alarmscheduler.GsonProvider.gson

object AlarmItemManager {
    private const val PREF_NAME = "AlarmItemsPref"

    fun saveAlarmItems(context: Context, alarmItems: MutableList<AlarmItem>, key: String) {
        val json = gson.toJson(alarmItems)
        val editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(key, json)
        editor.apply()
    }

    fun getAlarmItems(context: Context, key: String): MutableList<AlarmItem> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
        return gson.fromJson(json, type)
    }
}