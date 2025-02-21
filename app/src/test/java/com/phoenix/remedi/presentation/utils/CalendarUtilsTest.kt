package com.phoenix.remedi.presentation.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.phoenix.remedi.feature_alarms.presentation.utils.CalendarUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class CalendarUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp(){
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `24-hour format string list is formatted to 12-hour format`(){
        ShadowSettings.set24HourTimeFormat(false)
        val hourList = listOf("03:06","17:54", "19:36")
        val formattedList = CalendarUtils.formatStringHourList(hourList, context)

        assertThat(formattedList).containsExactly("03:06 AM", "05:54 PM", "07:36 PM")
    }

    @Test
    fun `24-hour format string list returns the same list when system is in 24-hour format`(){
        ShadowSettings.set24HourTimeFormat(true)
        val hourList = listOf("03:06","17:54", "19:36")
        val formattedList = CalendarUtils.formatStringHourList(hourList, context)

        assertThat(formattedList).isEqualTo(hourList)
    }
}