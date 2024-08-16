package com.phoenix.pillreminder.presentation.viewmodels

import com.google.common.truth.Truth.assertThat
import com.phoenix.pillreminder.data.repository.FakeMedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
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
class EditMedicinesViewModelTest {
    @Mock lateinit var repository: MedicineRepository
    @Mock lateinit var alarmScheduler: AndroidAlarmScheduler

    private lateinit var viewModel: EditMedicinesViewModel

    @Before
    fun init(){
        MockitoAnnotations.openMocks(this)
        viewModel = EditMedicinesViewModel(repository, alarmScheduler)
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
    fun `retrieves the milliseconds of all medicines with the same name`() = runTest{
        val fakeMedicineRepository = FakeMedicineRepository(
            mutableListOf(Medicine(1,
                "a",
                3f,
                "pill",
                1,
                123456789L,
                21,
                55,
                null,
                123456789L,
                987654321L,
                false,
                false,
                "Every day",
                1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "noID"),
                Medicine(2,
                    "test",
                    4f,
                    "pill",
                    1,
                    123456785L,
                    20,
                    33,
                    null,
                    123456788L,
                    887654321L,
                    false,
                    false,
                    "Every day",
                    1L,
                    medicinePeriodSet = true,
                    medicineNeedsReschedule = false,
                    "noID"),
                Medicine(3,
                    "test",
                    2f,
                    "pomade",
                    1,
                    123436800L,
                    15,
                    33,
                    null,
                    113456788L,
                    837654321L,
                    false,
                    false,
                    "Every day",
                    1L,
                    medicinePeriodSet = true,
                    medicineNeedsReschedule = false,
                    "noID"))
        )
        val fakeViewModel = EditMedicinesViewModel(fakeMedicineRepository, alarmScheduler)
        val result = fakeViewModel.getMillisList("test")

        val expectedResult = fakeMedicineRepository.medicinesList.filter{it.name == "test"}.map { it.alarmInMillis }

        assertThat(result).isNotEmpty()
        assertThat(result).hasSize(2)
        assertThat(expectedResult).isEqualTo(result)

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

    @Test
    fun testConvertMillisToAlarmHourList() {
        // Sample list of milliseconds representing different times.
        val longList = listOf(
            1723680000000L, // Aug. 15 2024 21:00:00 UTC (-3)
            1723683600000L, // Aug. 15 2024 01:00:00 UTC (-3)
            1723687200000L, // Aug. 15 2024 02:00:00 UTC (-3)
            1723680000000L  // duplicate, same as first value
        )

        val result24Hour = viewModel.convertMillisToAlarmHourList(longList, "HH:mm")
        val expectedResult24Hour = listOf(
            AlarmHour("21:00"),
            AlarmHour("22:00"),
            AlarmHour("23:00")
        )
        assertThat(expectedResult24Hour).isEqualTo(result24Hour)

        val result12Hour = viewModel.convertMillisToAlarmHourList(longList, "hh:mm a")
        val expectedResult12Hour = listOf(
            AlarmHour("09:00 PM"),
            AlarmHour("10:00 PM"),
            AlarmHour("11:00 PM")
        )
        assertThat(expectedResult12Hour).isEqualTo(result12Hour)
    }
}