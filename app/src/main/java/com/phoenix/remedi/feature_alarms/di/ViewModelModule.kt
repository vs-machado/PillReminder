package com.phoenix.remedi.feature_alarms.di

import com.phoenix.remedi.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.remedi.feature_alarms.presentation.viewmodels.EditMedicinesViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    @Singleton
    fun provideEditMedicinesViewModel(
        alarmScheduler: AndroidAlarmScheduler
    ): EditMedicinesViewModel{
        return EditMedicinesViewModel(alarmScheduler)
    }
}