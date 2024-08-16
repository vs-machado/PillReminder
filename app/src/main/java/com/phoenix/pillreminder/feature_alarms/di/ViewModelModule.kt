package com.phoenix.pillreminder.feature_alarms.di

import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.viewmodels.EditMedicinesViewModel
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
        repository: MedicineRepository,
        alarmScheduler: AndroidAlarmScheduler
    ): EditMedicinesViewModel{
        return EditMedicinesViewModel(repository, alarmScheduler)
    }
}