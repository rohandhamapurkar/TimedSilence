package com.example.timedsilence

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStarts_withVibrateSelected() {
        // Verify that "Vibrate" is selected by default
        composeTestRule.onNodeWithText("Vibrate").assertIsSelected()
    }

    @Test
    fun clickingStartSilence_transitionsToActiveState() {
        // Click Start Silence
        composeTestRule.onNodeWithText("Start Silence").performClick()
        
        // Check if the UI transitions to "Silence Active"
        // Note: This assumes the permission is already granted or not blocking.
        // In a real device/emulator, DND permission might prompt.
        composeTestRule.onNodeWithText("Silence Active").assertExists()
    }
}
