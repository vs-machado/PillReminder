package com.phoenix.pillreminder.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.data.repository.MedicineRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.data.repository.SharedPreferencesRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.di.AppModule
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideTestDatabase(@ApplicationContext context: Context): MedicineDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            MedicineDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @Provides
    @Singleton
    fun provideMedicineRepository(db: MedicineDatabase): MedicineRepository {
        return MedicineRepositoryImpl(db.medicineDao())
    }

    @Provides
    @Singleton
    fun provideWorkManager(app: Application): WorkManager {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, config)
        return WorkManager.getInstance(app)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        medicineRepository: MedicineRepository,
        sharedPreferencesRepository: SharedPreferencesRepository,
        @ApplicationContext context: Context
    ): AlarmScheduler {
        return AndroidAlarmScheduler(medicineRepository, sharedPreferencesRepository, context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferencesRepository(@ApplicationContext context: Context): SharedPreferencesRepository {
        return SharedPreferencesRepositoryImpl(context)
    }
}