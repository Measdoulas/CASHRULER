package com.cashruler.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class BaseComposeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    open fun setup() {
        hiltRule.inject()
    }

    /**
     * Attendre que l'interface utilisateur soit stable (plus d'animations en cours)
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }
    
    /**
     * Attendre un délai spécifique en millisecondes
     */
    protected fun waitFor(timeMillis: Long) {
        Thread.sleep(timeMillis)
    }

    protected fun getString(id: Int): String {
        return composeTestRule.activity.getString(id)
    }
}
