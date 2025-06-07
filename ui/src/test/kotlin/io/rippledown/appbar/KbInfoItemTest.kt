package io.rippledown.appbar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.KBInfo
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class KbInfoItemTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val kbInfo = KBInfo("gl123", "Glucose")

    private lateinit var handler: KbSelectionHandler

    @Before
    fun setup() {
        handler = mockk<KbSelectionHandler>()
    }

    @Test
    fun `should display kb name`() {
        with(composeTestRule) {
            setContent {
                KbInfoItem("Bondi", handler)
            }
            with(composeTestRule) {
                waitUntilExactlyOneExists(hasText("Bondi"))
            }
        }
    }

    @Test
    fun `should call handler when a kb name is selected`() {
        with(composeTestRule) {
            setContent {
                KbInfoItem(kbInfo.name, handler)
            }
            onNodeWithText(kbInfo.name).performClick()
            verify { handler.onSelect() }
        }
    }
}