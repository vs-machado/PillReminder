package com.phoenix.pillreminder.presentation.activities

import android.content.Context
import android.view.InputDevice
import android.view.View
import android.widget.NumberPicker
import android.widget.TimePicker
import androidx.navigation.NavController
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.phoenix.pillreminder.R
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.di.AppModule
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@HiltAndroidTest
@UninstallModules(AppModule::class)
@RunWith(AndroidJUnit4::class)
class MainActivityUITest {
    private lateinit var context: Context
    private lateinit var navController: NavController
    @Inject lateinit var database: MedicineDatabase

    companion object {
        private var tutorialSkipped = false
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    @Before
    fun setUp(){
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        navController = TestNavHostController(context)

        when(tutorialSkipped) {
            false -> {
                tutorialSkipped = skipTutorial()
            }
            true -> {}
        }
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
    }

    // This test doesn't skip the AppIntro activity. Before running the test, open the app and skip the intro.
    @Test
    fun registered_medicine_is_displayed_at_home_fragment() {
        createPillReminder()

        Thread.sleep(2000) // Wait for 2 seconds

        // Check if RecyclerView is displayed in HomeFragment
        onView(withId(R.id.rvMedicinesList)).check(matches(isDisplayed()))

        // Check if RecyclerView has exactly one item
        onView(withId(R.id.rvMedicinesList))
            .check(matches(hasChildCount(1)))

    }

    @Test
    fun registered_medicine_is_displayed_at_my_medicines_fragment() {
        createPillReminder()

        // Go to MyMedicinesFragment
        onView(withId(R.id.bottom_medicines)).perform(click())

        // Check if RecyclerView is being displayed
        onView(withId(R.id.rvMedicinesData)).check(matches(isDisplayed()))

        // Check if the registered medicine is shown in the RecyclerView
        onView(withId(R.id.rvMedicinesData))
            .check(matches(hasChildCount(1)))
         
    }
    
    @Test
    fun navigate_to_medicine_details_fragment_when_clicking_on_a_medicine_item_in_my_medicines_fragment() {
        createPillReminder()

        // Go to MyMedicinesFragment
        onView(withId(R.id.bottom_medicines)).perform(click())

        // Check if RecyclerView is being displayed
        onView(withId(R.id.rvMedicinesData)).check(matches(isDisplayed()))

        // Check if the registered medicine is shown in the RecyclerView
        onView(withId(R.id.rvMedicinesData))
            .check(matches(hasChildCount(1)))
        
        // Click on the first item in the RecyclerView
        onView(withId(R.id.rvMedicinesData))
            .perform(actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("test")),
                click()
            ))

        // Verify that we've navigated to the MedicineDetailsFragment
        onView(withId(R.id.tvNameMed))
            .check(matches(isDisplayed()))
            .check(matches(withText("test")))
        
    }

    @Test
    fun user_can_change_the_app_language() {
        // Open the settings fragment
        onView(withId(R.id.settings)).perform(click())

        // Select the language preference
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                actionOnItem<RecyclerView.ViewHolder>(
                  hasDescendant(withText(R.string.app_language)), click()
                )
            )


        // Select Português (Brasil) as preferred language
        onView(withText("Português (Brasil)"))
            .inRoot(isDialog())
            .perform(click())

        // Ensure that the language preference has been updated
        onView(withId(androidx.preference.R.id.recycler_view))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("Português (Brasil)"),
                            isDisplayed()
                        )
                    )
                )
            )
    }

    private fun createPillReminder(){
        // Click to add medicine
        onView(withId(R.id.fabAddMedicine)).perform(click())

        // Navigates to fragment_add_medicines and display the medicine name form
        onView(withId(R.id.tietMedicineName)).check(matches(isDisplayed()))

        // Navigates to fragment_add_medicines and display the medicine name form
        onView(withId(R.id.tietMedicineName)).perform(typeText("test"), closeSoftKeyboard())

        // fabNext (initially invisible) should be visible
        onView(withId(R.id.fabNext)).check(matches(isDisplayed()))

        // Navigates to fragment_medicine_form
        onView(withId(R.id.fabNext)).perform(click())

        // Select medicine form "Pill" and navigates to fragment_quantity_and_strength
        onData(anything())
            .inAdapterView(withId(R.id.lvMedForm))
            .atPosition(0)
            .perform(click())

        // Input the medicine quantity
        onView(withId(R.id.etQuantity)).perform(typeText("1"), closeSoftKeyboard())

        // fabNext (initially invisible) should be visible
        onView(withId(R.id.fabNext)).check(matches(isDisplayed()))

        // Navigates to fragment_frequency
        onView(withId(R.id.fabNext)).perform(click())

        // Select frequency "Every day" and navigates to fragment_how_many_per_day
        onData(anything())
            .inAdapterView(withId(R.id.lvFrequency))
            .atPosition(0)
            .perform(click())

        // Input the number of doses per day
        onView(withId(R.id.npHowOften)).perform(setNumberPickerValue(0))

        // Navigates to fragment_alarm_hour
        onView(withId(R.id.fabNext)).perform(click())

        // Set the alarm time and navigate to fragment_treatment_duration
        onView(withId(R.id.tpAlarm)).perform(setTime(hour = 22, minute = 32))
        onView(withId(R.id.fabNext)).perform(click())

        // Set a temporary treatment duration
        onData(anything())
            .inAdapterView(withId(R.id.lvTreatmentDuration))
            .atPosition(1)
            .perform(click())
    }

    private fun skipTutorial(): Boolean{
        // Click on skip tutorial button
        onView(isRoot()).perform(clickLeftBottomCorner())

        // Don't show the dialog again
        onView(withId(R.id.cbDontShowAgain))
            .inRoot(isDialog())
            .perform(click())

        // Dismiss overlay permission dialog
        onView(withId(R.id.btnDismissRequest))
            .inRoot(isDialog())
            .perform(click())

        return true
    }

    private fun setNumberPickerValue(value: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(NumberPicker::class.java)
            }

            override fun getDescription(): String {
                return "Set the value of a NumberPicker"
            }

            override fun perform(uiController: UiController, view: View) {
                (view as NumberPicker).value = value
            }
        }
    }

    private fun setTime(hour: Int, minute: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TimePicker::class.java)
            }

            override fun getDescription(): String {
                return "Set time on TimePicker"
            }

            override fun perform(uiController: UiController, view: View) {
                val timePicker = view as TimePicker
                timePicker.hour = hour
                timePicker.minute = minute
            }
        }
    }

    private fun clickLeftBottomCorner(): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Click on the left bottom corner of the screen"
            override fun perform(uiController: UiController, view: View) {
                val clickAction = GeneralClickAction(
                    Tap.SINGLE,
                    LeftBottomCornerProvider(),
                    Press.FINGER,
                    InputDevice.SOURCE_UNKNOWN,
                    0
                )
                clickAction.perform(uiController, view)
            }
        }
    }

    private class LeftBottomCornerProvider : CoordinatesProvider {
        override fun calculateCoordinates(view: View): FloatArray {
            val screenWidth = view.width
            val screenHeight = view.height
            val x = screenWidth * 0.1f // 10% from the left edge
            val y = screenHeight * 0.9f // 90% from the top (10% from the bottom)
            return floatArrayOf(x, y)
        }
    }
}