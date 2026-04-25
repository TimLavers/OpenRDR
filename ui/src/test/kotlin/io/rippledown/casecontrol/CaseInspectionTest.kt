package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createLargeViewableCaseWithInterpretation
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
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
