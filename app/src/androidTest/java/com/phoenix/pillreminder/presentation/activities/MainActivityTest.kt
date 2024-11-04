package com.phoenix.pillreminder.presentation.activities

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import javax.inject.Inject
import com.google.common.truth.Truth.assertThat
import com.phoenix.pillreminder.feature_alarms.presentation.utils.languageMapping

// Language tests needs to be run in a phone using English language as default
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    @Inject
    lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `ensure that language is set correctly in shared preferences`() {
        val phoneLanguage = Locale.getDefault().toLanguageTag()
        val language = languageMapping[phoneLanguage]
        val chosenLanguage = "pt-BR"

        // System's language
        assertThat(sharedPreferencesRepository.getAppLanguage()).isEqualTo("en")

        // User set language
        if (language != null) {
            sharedPreferencesRepository.setAppLanguage(chosenLanguage)
        }

        assertThat(sharedPreferencesRepository.getAppLanguage()).isEqualTo("pt-BR")
    }
}