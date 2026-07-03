package io.rippledown.interpretation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.rippledown.constants.interpretation.REPORT_PANEL
import io.rippledown.constants.interpretation.REPORT_TOGGLE
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class ReportViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show report toggle`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = null,
                    isVisible = false,
                    onToggle = {}
                )
            }

            onNodeWithContentDescription(REPORT_TOGGLE).assertIsDisplayed()
            onNodeWithText("Report").assertIsDisplayed()
        }
    }

    @Test
    fun `should show report panel when visible`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = "Test report content",
                    isVisible = true,
                    onToggle = {}
                )
            }

            onNodeWithContentDescription(REPORT_PANEL).assertIsDisplayed()
            onNodeWithText("Test report content").assertIsDisplayed()
        }
    }

    @Test
    fun `should hide report panel when not visible`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = "Test report content",
                    isVisible = false,
                    onToggle = {}
                )
            }

            onNodeWithContentDescription(REPORT_PANEL).assertDoesNotExist()
        }
    }

    @Test
    fun `should show loading message when loading`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = null,
                    isVisible = true,
                    isLoading = true,
                    onToggle = {}
                )
            }

            onNodeWithText("Generating report…").assertIsDisplayed()
        }
    }

    @Test
    fun `should show no report message when text is blank`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = "",
                    isVisible = true,
                    isLoading = false,
                    onToggle = {}
                )
            }

            onNodeWithText("No report.").assertIsDisplayed()
        }
    }

    @Test
    fun `should show no comments message when case has no comments`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = "",
                    isVisible = true,
                    isLoading = false,
                    hasComments = false,
                    onToggle = {}
                )
            }

            onNodeWithText("No comments to report on.").assertIsDisplayed()
        }
    }

    @Test
    fun `should call onToggle when toggle is clicked`() = runTest {
        var toggled = false
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = null,
                    isVisible = false,
                    onToggle = { toggled = true }
                )
            }

            onNodeWithContentDescription(REPORT_TOGGLE).performClick()
            waitForIdle()
        }
        assert(toggled)
    }
}
