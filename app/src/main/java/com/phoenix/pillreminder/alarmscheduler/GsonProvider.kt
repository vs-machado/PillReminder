package com.phoenix.pillreminder.alarmscheduler

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.phoenix.pillreminder.adapter.LocalDateTimeAdapter
import java.time.LocalDateTime

object GsonProvider {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
}