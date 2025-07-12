package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.utils.applicationFor
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
                CaseInspection(case, false, handler)
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
                CaseInspection(case, false, handler)
            }
            requireInterpretation(text)
        }
    }
}

fun main() {
    val case = createViewableCase(name = "Bondi", caseId = 45L)
    applicationFor {
        CaseInspection(case, false, mockk())
    }
}
