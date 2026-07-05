package io.rippledown.interpretation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.chat.TYPING_INDICATOR
import io.rippledown.constants.interpretation.REPORT_DISCLAIMER_ICON
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
            // Two nodes carry the text: the visible markdown render and the
            // hidden, zero-size accessibility mirror that exposes the raw text
            // to the Java accessibility bridge. Assert the content is present.
            onAllNodesWithText("Test report content").onFirst().assertExists()
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
    fun `should show typing indicator when loading`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = null,
                    isVisible = true,
                    isLoading = true,
                    onToggle = {}
                )
            }

            onNodeWithContentDescription(TYPING_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `should hide typing indicator once loading completes`() = runTest {
        with(composeTestRule) {
            var isLoading by mutableStateOf(true)
            setContent {
                ReportView(
                    reportText = if (isLoading) null else "Test report content",
                    isVisible = true,
                    isLoading = isLoading,
                    onToggle = {}
                )
            }

            onNodeWithContentDescription(TYPING_INDICATOR).assertIsDisplayed()

            // Loading completes and the report arrives.
            isLoading = false

            onNodeWithContentDescription(TYPING_INDICATOR).assertDoesNotExist()
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

    @Test
    fun `should show info icon for AI disclaimer`() = runTest {
        with(composeTestRule) {
            setContent {
                ReportView(
                    reportText = null,
                    isVisible = false,
                    onToggle = {}
                )
            }

            onNodeWithContentDescription(REPORT_DISCLAIMER_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `formatReportText should convert powers of 10 to superscripts and micro prefix to mu`() {
        formatReportText("10^12") shouldBe "10¹²"
        formatReportText("10^9") shouldBe "10⁹"
        formatReportText("10^-3") shouldBe "10⁻³"
        formatReportText("The count is 10^6/L") shouldBe "The count is 10⁶/L"
        formatReportText("10^0") shouldBe "10⁰"
        formatReportText("Multiple: 10^3 and 10^-5") shouldBe "Multiple: 10³ and 10⁻⁵"
        formatReportText("umol/L") shouldBe "μmol/L"
        formatReportText("Creatinine 120 umol/L (10^6/L)") shouldBe "Creatinine 120 μmol/L (10⁶/L)"
        formatReportText("No change") shouldBe "No change"
    }
}
