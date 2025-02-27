package com.phoenix.remedi.feature_alarms.presentation.utils

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LanguageConfig {
    companion object {
        fun changeLanguage(languageCode: String, activity: Activity? = null) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}

val languageMapping = mapOf(
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
    "es-PA" to "es", "es-UY" to "es", "es" to "es", "es-US" to "es",
    // Italian regions
    "it-IT" to "it", "it-CH" to "it", "it-SM" to "it", "it" to "it"
)