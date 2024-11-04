package com.phoenix.pillreminder.presentation.viewmodels

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.EditMedicinesViewModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings

//@RunWith(RobolectricTestRunner::class)
//class EditMedicinesViewModelRobolectricTest {
//    @Mock
//    lateinit var mockContext: Context
//
//    @Mock
//    lateinit var alarmScheduler: AndroidAlarmScheduler
//
//    private lateinit var viewModel: EditMedicinesViewModel
//    private lateinit var longList: List<Long>
//
//    @Before
//    fun init(){
//        MockitoAnnotations.openMocks(this)
//        viewModel = EditMedicinesViewModel(alarmScheduler)
//        mockContext = mock(Context::class.java)
//
//        // Sample list of milliseconds representing different times.
//        longList = listOf(
//            1723680000000L, // Aug. 15 2024 21:00:00 UTC (-3)
//            1723683600000L, // Aug. 15 2024 01:00:00 UTC (-3)
//            1723687200000L, // Aug. 15 2024 02:00:00 UTC (-3)
//            1723680000000L  // duplicate, same as first value
//        )
//    }
//
//    @Test
//    fun testConvertMillisToAlarmHourList_24HourFormat() {
//        ShadowSettings.set24HourTimeFormat(true)
//
//        val result24Hour = viewModel.convertMillisToAlarmHourList(mockContext, longList)
//        val expectedResult24Hour = listOf(
//            AlarmHour("21:00"),
//            AlarmHour("22:00"),
//            AlarmHour("23:00")
//        )
//        assertThat(expectedResult24Hour).isEqualTo(result24Hour)
//    }
//
//    @Test
//    fun testConvertMillisToAlarmHourList_12HourFormat(){
//        ShadowSettings.set24HourTimeFormat(false)
//        val result12Hour = viewModel.convertMillisToAlarmHourList(mockContext, longList)
//        val expectedResult12Hour = listOf(
//            AlarmHour("09:00 PM"),
//            AlarmHour("10:00 PM"),
//            AlarmHour("11:00 PM")
//        )
//        assertThat(expectedResult12Hour).isEqualTo(result12Hour)
//    }
//}