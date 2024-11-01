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

// Language tests needs to be run in a phone using English language as default
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var languageMapping: Map<String, String>

    @Inject
    lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        languageMapping = mapOf(
            // English regions
            "en-US" to "en", "en-GB" to "en", "en-AU" to "en",
            "en-CA" to "en", "en-IN" to "en", "en-NZ" to "en",
            "en-ZA" to "en", "en-IE" to "en", "en-JM" to "en",
            "en" to "en",
            // Portuguese region
            "pt-BR" to "pt-BR",
            // Spanish regions
            "es-ES" to "es", "es-MX" to "es", "es-AR" to "es",
            "es-CO" to "es", "es-CL" to "es", "es-PE" to "es",
            "es-VE" to "es", "es-EC" to "es", "es-GT" to "es",
            "es-CU" to "es", "es-BO" to "es", "es-DO" to "es",
            "es-HN" to "es", "es-PY" to "es", "es-SV" to "es",
            "es-NI" to "es", "es-CR" to "es", "es-PR" to "es",
            "es-PA" to "es", "es-UY" to "es", "es" to "es",
            // Italian regions
            "it-IT" to "it", "it-CH" to "it", "it-SM" to "it", "it" to "it"
        )
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