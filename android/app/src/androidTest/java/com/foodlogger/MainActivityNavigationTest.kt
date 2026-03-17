package com.foodlogger

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun bottomNavigation_switchesBetweenXmlScreens() {
        onView(withText(R.string.title_inventory)).check(matches(isDisplayed()))

        onView(withText(R.string.title_recipes)).perform(click())
        onView(withText(R.string.action_add_recipe)).check(matches(isDisplayed()))

        onView(withText(R.string.title_barcode)).perform(click())
        onView(withText(R.string.action_search_product)).check(matches(isDisplayed()))
    }
}