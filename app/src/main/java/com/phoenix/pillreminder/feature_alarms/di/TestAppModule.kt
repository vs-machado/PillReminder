package com.phoenix.pillreminder.feature_alarms.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.data.repository.MedicineRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmReceiver
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
        return WorkManager.getInstance(app)
    }

    @Provides
    @Singleton
    fun provideAlarmReceiver(): AlarmReceiver {
        return AlarmReceiver()
    }

}