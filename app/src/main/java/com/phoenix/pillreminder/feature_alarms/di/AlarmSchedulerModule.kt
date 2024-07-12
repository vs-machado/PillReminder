package com.phoenix.pillreminder.feature_alarms.di

import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmSchedulerModule {
    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(androidAlarmScheduler: AndroidAlarmScheduler): AlarmScheduler
}