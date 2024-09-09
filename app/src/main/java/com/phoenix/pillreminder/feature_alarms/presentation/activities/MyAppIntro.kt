package com.phoenix.pillreminder.feature_alarms.presentation.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
import com.phoenix.pillreminder.R

class MyAppIntro: AppIntro2() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationContext.apply{
            // Call addSlide passing your Fragments.
            // You can use AppIntroFragment to use a pre-built fragment
            addSlide(AppIntroFragment.createInstance(
                title = getString(R.string.welcome),
                description = getString(R.string.app_explanation),
                imageDrawable = R.drawable.pill_intro,
                titleColorRes = R.color.intro_text_blue,
                descriptionColorRes = R.color.intro_text_blue,
                backgroundColorRes = R.color.gradient_end_color,
                titleTypefaceFontRes = R.font.manrope_semibold,
                descriptionTypefaceFontRes = R.font.manrope_semibold
            ))
            addSlide(
                AppIntroFragment.createInstance(
                    title = getString(R.string.app_needs_permissions),
                    description = getString(R.string.notification_mandatory),
                    backgroundColorRes = R.color.gradient_end_color,
                    imageDrawable = R.drawable.ic_permission_storyset_painted,
                    titleColorRes = R.color.intro_text_blue,
                    descriptionColorRes = R.color.intro_text_blue,
                    titleTypefaceFontRes = R.font.manrope_semibold,
                    descriptionTypefaceFontRes = R.font.manrope_semibold
                ))
            addSlide(
                AppIntroFragment.createInstance(
                    title = getString(R.string.pillbox_reminders),
                    description = getString(R.string.click_on_pillbox),
                    imageDrawable = R.drawable.tutorial_pillbox_reminders,
                    backgroundColorRes = R.color.gradient_end_color,
                    titleColorRes = R.color.intro_text_blue,
                    descriptionColorRes = R.color.intro_text_blue,
                    titleTypefaceFontRes = R.font.manrope_semibold,
                    descriptionTypefaceFontRes = R.font.manrope_semibold
                )
            )
            addSlide(
                AppIntroFragment.createInstance(
                    title = getString(R.string.app_usage),
                    description = getString(R.string.just_click_on_plus_btn),
                    imageDrawable = R.drawable.tutorial_add_medicine,
                    backgroundColorRes = R.color.gradient_end_color,
                    titleColorRes = R.color.intro_text_blue,
                    descriptionColorRes = R.color.intro_text_blue,
                    titleTypefaceFontRes = R.font.manrope_semibold,
                    descriptionTypefaceFontRes = R.font.manrope_semibold
                )
            )
            setTransformer(AppIntroPageTransformerType.Parallax(
                titleParallaxFactor = 1.0,
                imageParallaxFactor = -1.0,
                descriptionParallaxFactor = 2.0
            ))

            isColorTransitionsEnabled = true

            setProgressIndicator()
            setNavBarColorRes(R.color.gradient_end_color)
            showStatusBar(true)
            setStatusBarColorRes(R.color.gradient_end_color)

            askForPermissions(
                permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                slideNumber = 2,
                required = true
            )
        }

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        finish()
    }
}