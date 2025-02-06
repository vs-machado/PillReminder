package com.phoenix.pillreminder.feature_alarms.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.data.repository.MedicineRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.data.repository.SharedPreferencesRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMedicineDatabase(app: Application): MedicineDatabase {
        return Room.databaseBuilder(
            app,
            MedicineDatabase::class.java,
            "medicine_data_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMedicineRepository(db: MedicineDatabase): MedicineRepository{
        return MedicineRepositoryImpl(db.medicineDao())
    }

    @Provides
    @Singleton
    fun provideWorkManager(app: Application): WorkManager{
        return WorkManager.getInstance(app)
    }

    @Provides
    @Singleton
    fun provideAlarmReceiver(): AlarmReceiver {
        return AlarmReceiver()
    }

    @Provides
    @Singleton
    fun provideSharedPreferencesRepository(@ApplicationContext context: Context): SharedPreferencesRepository{
        return SharedPreferencesRepositoryImpl(context)
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
}