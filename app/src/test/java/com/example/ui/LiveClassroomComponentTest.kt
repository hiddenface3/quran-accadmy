package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.utils.AudioRoute
import com.example.data.model.QuranVerse
import com.example.ui.screens.classroom.LiveClassControlBar
import com.example.ui.screens.classroom.QuranCompanionCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveClassroomComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testQuranCompanionCardPagination() {
        var prevClicked = false
        var nextClicked = false
        var closeClicked = false

        val verse = QuranVerse(
            surahNumber = 67,
            surahName = "Al-Mulk",
            ayahNumber = 1,
            arabicText = "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ",
            transliteration = "Tabaraka allathee biyadihi almulku",
            translation = "Blessed is He in whose hand is dominion",
            tajweedNote = "Ghunnah on Nun"
        )

        composeTestRule.setContent {
            QuranCompanionCard(
                verse = verse,
                currentIndex = 0,
                totalCount = 30,
                onPrev = { prevClicked = true },
                onNext = { nextClicked = true },
                onClose = { closeClicked = true }
            )
        }

        // Verify Arabic text displayed
        composeTestRule.onNodeWithText("تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ").assertIsDisplayed()

        // Verify pagination
        composeTestRule.onNodeWithContentDescription("Next").performClick()
        assertTrue("Next should be clicked", nextClicked)

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue("Close should be clicked", closeClicked)
    }

    @Test
    fun testLiveClassControlBarInteractions() {
        var micToggled = false
        var cameraToggled = false
        var handRaiseToggled = false
        var chatToggled = false
        var leaveClicked = false

        composeTestRule.setContent {
            LiveClassControlBar(
                isMicMuted = false,
                isCameraOn = true,
                currentAudioRoute = AudioRoute.SPEAKER,
                isHandRaised = false,
                isInClassChatOpen = false,
                onToggleMic = { micToggled = true },
                onToggleCamera = { cameraToggled = true },
                onSelectAudioRoute = {},
                onToggleHandRaise = { handRaiseToggled = true },
                onToggleChat = { chatToggled = true },
                onEnterPip = {},
                onLeaveClass = { leaveClicked = true },
                connectionQuality = 3
            )
        }

        // Tap Mute Mic button
        composeTestRule.onNodeWithText("Mute").performClick()
        assertTrue("Mic should be toggled", micToggled)

        // Tap Stop Cam button
        composeTestRule.onNodeWithText("Stop Cam").performClick()
        assertTrue("Camera should be toggled", cameraToggled)

        // Tap Raise Hand button
        composeTestRule.onNodeWithText("Raise").performClick()
        assertTrue("Hand raise should be toggled", handRaiseToggled)

        // Tap Chat button
        composeTestRule.onNodeWithText("Chat").performClick()
        assertTrue("Chat should be toggled", chatToggled)

        // Tap Leave button
        composeTestRule.onNodeWithText("Leave").performClick()
        assertTrue("Leave should be clicked", leaveClicked)
    }
}
