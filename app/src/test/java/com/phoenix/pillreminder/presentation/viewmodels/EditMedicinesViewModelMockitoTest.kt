package com.phoenix.pillreminder.presentation.viewmodels

import com.google.common.truth.Truth.assertThat
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.EditMedicinesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
class EditMedicinesViewModelMockitoTest {
    @Mock lateinit var alarmScheduler: AndroidAlarmScheduler

    private lateinit var viewModel: EditMedicinesViewModel

    @Before
    fun init(){
        MockitoAnnotations.openMocks(this)
        viewModel = EditMedicinesViewModel(alarmScheduler)
    }

    @Test
    fun `retrieves the date formatted with timezone offset`() = runTest {
        val selectedDateMillis = 1723680000000L // Aug. 15 2024 00:00:00 UTC
        val lastAlarmOfTheDayMillis = 51825000L // 14:23:45 UTC
        val timeZone = TimeZone.getTimeZone("GMT-3")

        val result = viewModel.formatSelectedDateWithOffset(selectedDateMillis, lastAlarmOfTheDayMillis, timeZone)

        assertThat(result).isEqualTo(1723742625000L) // Aug. 15 2024 14:23:45 UTC -3
    }

    @Test
    fun `formats time correctly in 24-hour format`() = runTest {
        val result = viewModel.formatTime(14, 30, true)
        assertThat(result).isEqualTo("14:30")
    }

    @Test
    fun `formats time correctly in 12-hour format`() = runTest {
        val result = viewModel.formatTime(14, 30, false)
        assertThat(result).isEqualTo("02:30 PM")
    }

    @Test
    fun `splits 24-hour format alarm time into two integer variables hours and minutes`() = runTest {
        val result = viewModel.parseAlarmTime("13:20")
        assertThat(result).isEqualTo(Pair(13, 20))
    }

    @Test
    fun `splits 12-hour format alarm time into two integer variables hours and minutes`() = runTest {
        val result = viewModel.parseAlarmTime("05:35 PM")
        assertThat(result).isEqualTo(Pair(17, 35))
    }

    @Test
    fun `resets a calendar date millis to midnight considering the offset`() = runTest {
        val hourMillis = 1723701600000L // Aug. 15 2024 06:00:00 UTC
        val timeZone = TimeZone.getTimeZone("GMT-3")
        val result = viewModel.resetCalendarHourToMidnight(hourMillis, timeZone)
        val expectedResult = 1723690800000L // Aug. 15 2024 00:00:00 UTC (-3)

        assertThat(result).isEqualTo(expectedResult)
    }


}