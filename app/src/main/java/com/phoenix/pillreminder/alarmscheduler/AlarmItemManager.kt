package com.phoenix.pillreminder.alarmscheduler

import android.content.Context
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.phoenix.pillreminder.alarmscheduler.GsonProvider.gson

object AlarmItemManager {
    private const val PREF_NAME = "AlarmItemsPref"
    private const val KEY_ALARM_ITEMS = "alarmItems"

    fun saveAlarmItems(context: Context, alarmItems: MutableList<AlarmItem>) {
        val json = gson.toJson(alarmItems)
        Log.i("JSON", json)
        val editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(KEY_ALARM_ITEMS, json)
        editor.apply()
    }

    fun getAlarmItems(context: Context): MutableList<AlarmItem> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALARM_ITEMS, null)
        val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
        return gson.fromJson(json, type)
    }
}