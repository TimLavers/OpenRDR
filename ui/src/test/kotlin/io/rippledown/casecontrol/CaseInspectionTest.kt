package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.utils.applicationFor
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
        handler = mockk<CaseInspectionHandler>(relaxed = true)
    }

    @Test
    fun `should show case view`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCase(caseId)

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
        val case = createCaseWithInterpretation(name = "case a", id = 1L, conclusionTexts = listOf(text))
        with(composeTestRule) {
            setContent {
                CaseInspection(case, false, handler)
            }
            requireInterpretation(text)
        }
    }
}

fun main() {
    val case = createCase(name = "Bondi", id = 45L)
    applicationFor {
            CaseInspection(case, false, object : CaseInspectionHandler {
                override fun swapAttributes(moved: Attribute, target: Attribute) {}
            })
    }
}
