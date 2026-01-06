package io.rippledown.appbar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.chat.clickChatIconToggle
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_ID
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.model.KBInfo
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class ApplicationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bondiInfo = KBInfo("Bondi")

    lateinit var handler: AppBarHandler

    @Before
    fun setUp() {
        handler = mockk<AppBarHandler>(relaxed = true)
        every { handler.kbList } returns { emptyList() }
    }

    @Test
    fun `should show current KB name`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(bondiInfo, handler = handler)
            }
            assertKbNameIs(bondiInfo.name)
            onNodeWithTag(testTag = KB_NAME_ID).assertExists()
        }
    }

    @Test
    fun `should show KB selector if not rule building`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(bondiInfo, handler = handler)
            }
            onNodeWithTag(testTag = KB_CONTROL_ID, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `should remove KB selector if rule building`() {
        every { handler.isRuleSessionInProgress } returns true
        with(composeTestRule) {
            setContent {
                ApplicationBar(KBInfo("Bondi"), handler = handler)
            }
            onNodeWithTag(testTag = KB_CONTROL_ID).assertDoesNotExist()
        }
    }

    @Test
    fun semantics() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(KBInfo("Bondi"), handler = handler)
            }
            onNodeWithContentDescription(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION).assertExists()
        }
    }

    @Test
    fun `should call handler if the the chat icon toggle is clicked and the icon is enabled`() {
        with(composeTestRule) {
            //Given
            setContent {
                ApplicationBar(KBInfo("Bondi"), isChatEnabled = true, handler = handler)
            }
            //When
            clickChatIconToggle()

            //Then
            verify { handler.onToggleChat() }
        }
    }

    @Test
    fun `should not call handler if the the chat icon toggle is clicked and the icon is disabled`() {
        with(composeTestRule) {
            //Given
            setContent {
                ApplicationBar(KBInfo("Bondi"), isChatEnabled = false, handler = handler)
            }
            //When
            clickChatIconToggle()

            //Then
            verify(exactly = 0) { handler.onToggleChat() }
        }
    }
}

fun main() {
    val bondiInfo = KBInfo("Bondi")
    val handler = mockk<AppBarHandler>()
    every { handler.kbList } returns { emptyList() }

    application {
        Window(onCloseRequest = ::exitApplication) {
            ApplicationBar(bondiInfo, handler = handler)
        }
    }
}
