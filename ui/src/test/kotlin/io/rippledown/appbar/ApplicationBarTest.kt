package io.rippledown.appbar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.model.KBInfo
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class ApplicationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val bondiInfo = KBInfo("Bondi")

    lateinit var handler: AppBarHandler

    @Before
    fun setUp() {
        handler = mockk<AppBarHandler>(relaxed = true)
        every { handler.kbList } returns { emptyList() }
    }
    @Test
    fun `should show application heading`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(bondiInfo, handler)
            }
            onNodeWithTag(testTag = MAIN_HEADING_ID).assertExists()
        }
    }

    @Test
    fun `should show current KB name`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(bondiInfo, handler)
            }
            assertKbNameIs(bondiInfo.name)
        }
    }

    @Test
    fun `should show KB selector if not rule building`() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(bondiInfo, handler)
            }
            onNodeWithTag(testTag = KB_SELECTOR_ID, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `should remove KB selector if rule building`() {
        every { handler.isRuleSessionInProgress } returns true
        with(composeTestRule) {
            setContent {
                ApplicationBar(KBInfo("Bondi"), handler)
            }
            onNodeWithTag(testTag = KB_SELECTOR_ID).assertDoesNotExist()
        }
    }

    @Test
    fun semantics() {
        with(composeTestRule) {
            setContent {
                ApplicationBar(KBInfo("Bondi"), handler)
            }
            onNodeWithContentDescription(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION).assertExists()
        }
    }
}

fun main() {
    val bondiInfo = KBInfo("Bondi")
    val handler = mockk<AppBarHandler>(relaxed = true)
    every { handler.kbList } returns { emptyList() }

    application {
        Window(onCloseRequest = ::exitApplication) {
            ApplicationBar(bondiInfo, handler)
        }
    }
}
