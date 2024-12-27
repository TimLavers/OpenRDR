package io.rippledown.appbar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.rippledown.constants.main.MAIN_HEADING_ID
import org.junit.Rule
import kotlin.test.Test

class ApplicationNameDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show application heading`() {
        with(composeTestRule) {
            setContent {
                ApplicationNameDisplay()
            }
            onNodeWithTag(testTag = MAIN_HEADING_ID).assertExists()
        }
    }
}