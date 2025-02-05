package io.rippledown.appbar

import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.model.KBInfo
import org.junit.Rule
import kotlin.test.Test

class KbNameDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val bondiInfo = KBInfo("Bondi")

    @Test
    fun `should show KB name`() {
        with(composeTestRule) {
            setContent {
                KbNameDisplay(bondiInfo)
            }
            assertKbNameIs(bondiInfo.name)
        }
    }

    @Test
    fun `should show message if no KB is open`() {
        with(composeTestRule) {
            setContent {
                KbNameDisplay(null)
            }
            assertKbNameIs("No KB selected")
        }
    }
}