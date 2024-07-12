package com.phoenix.pillreminder.feature_alarms.di

import android.content.Context
import com.phoenix.pillreminder.feature_alarms.data.repository.SharedPreferencesRepositoryImpl
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {
    @Provides
    @Singleton
    fun provideSharedPreferencesRepository(@ApplicationContext context: Context): SharedPreferencesRepository{
        return SharedPreferencesRepositoryImpl(context)
    }
}