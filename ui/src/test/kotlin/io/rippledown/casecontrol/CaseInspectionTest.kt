package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeLessThan
import io.mockk.mockk
import io.rippledown.caseview.dateCellContentDescription
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.utils.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseInspectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseInspectionHandler

    @Before
    fun setUp() {
        handler = mockk<CaseInspectionHandler>()
    }

    @Test
    fun `should show case view`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createViewableCase(caseId)

        with(composeTestRule) {
            setContent {
                CaseInspection(case, handler = handler)
            }
            waitForCaseToBeShowing(caseName)
        }
    }

    @Test
    fun `should show interpretation`() = runTest {
        val text = "Go to Bondi now!"
        val case = createViewableCaseWithInterpretation(name = "case a", caseId = 1L, conclusionTexts = listOf(text))
        with(composeTestRule) {
            setContent {
                CaseInspection(case, handler = handler)
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `should auto-scroll to show the most recent episode for a multi-episode case`() = runTest {
        // Build a case with two episodes for the same attribute.
        val rdrCase = with(RDRCaseBuilder()) {
            addValue(glucose, lastWeek, "OLD_VALUE")
            addValue(glucose, today, "RECENT_VALUE")
            build("multi", 1L)
        }
        val case = ViewableCase(rdrCase, CaseViewProperties(listOf(glucose)))

        with(composeTestRule) {
            setContent {
                // Constrain the panel so that the multi-episode table is
                // wider than the viewport and horizontal scrolling is needed
                // to bring the most recent episode into view.
                Box(modifier = Modifier.size(width = 300.dp, height = 500.dp)) {
                    CaseInspection(case, handler = handler)
                }
            }
            // After auto-scrolling fully right, the most-recent episode's
            // date cell (index 1) should be rendered near the start of the
            // scrollable area (just past the fixed attribute column on the
            // left). Without auto-scrolling it would render one full
            // episode-block-width further to the right, well into the
            // second half of the panel. Bounds are reported with the scroll
            // offset applied, so a small left value here proves the
            // auto-scroll fired.
            val date1Bounds = onNodeWithContentDescription(dateCellContentDescription(1))
                .assertIsDisplayed()
                .getBoundsInRoot()
            date1Bounds.left shouldBeLessThan 150.dp
        }
    }

    @Test
    fun `interpretation should remain visible when the case has many attributes`() = runTest {
        val text = "Go to Bondi now!"
        val case = createLargeViewableCaseWithInterpretation(
            name = "case a",
            caseId = 1L,
            numberOfAttributes = 80,
            conclusionTexts = listOf(text)
        )
        with(composeTestRule) {
            setContent {
                Box(modifier = Modifier.size(width = 600.dp, height = 500.dp)) {
                    CaseInspection(case, handler = handler)
                }
            }
            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD, useUnmergedTree = true)
                .assertIsDisplayed()
            requireInterpretation(text)
        }
    }
}

fun main() {
    val case = createViewableCase(name = "Bondi", caseId = 45L)
    applicationFor {
        CaseInspection(case, handler = mockk())
    }
}
