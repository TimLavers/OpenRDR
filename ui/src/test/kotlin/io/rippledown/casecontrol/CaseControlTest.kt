package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.Attribute
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.rule.clickCancelRuleButton
import io.rippledown.rule.requireRuleMakerToBeDisplayed
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseControlHandler

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>(relaxed = true)
    }


    @Test
    fun `should show the interpretative report of the case`() = runTest {
        val name = "case A"
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(name, 1, listOf(bondiComment))

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    conditionHints = listOf(),
                    handler = handler
                )
            }

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show case view`() = runTest {
        val viewableCase = createCaseWithInterpretation(
            name = "case 1",
            caseId = 1,
            conclusionTexts = listOf()
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            waitForCaseToBeShowing("case 1")
        }
    }

    @Test
    fun `should show rule builder if the cornerstone status is not null`() = runTest {
        val name = "Bondi"
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(name, 1, listOf(bondiComment))
        val cornerstone = createCaseWithInterpretation("Malabar", 1, listOf(bondiComment))
        val cornerstoneStatus = CornerstoneStatus(cornerstone, 42, 84)

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    cornerstoneStatus = cornerstoneStatus,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            requireInterpretation(bondiComment)

            //Then
            requireRuleMakerToBeDisplayed()
        }
    }
    @Test
    fun `should call handler when the rule session is cancelled`() = runTest {
        val name = "Bondi"
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(name, 1, listOf(bondiComment))

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    cornerstoneStatus = CornerstoneStatus(null, 42, 84),
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            requireInterpretation(bondiComment)
            requireRuleMakerToBeDisplayed()

            //when
            clickCancelRuleButton()

            // Then
            verify { handler.endRuleSession() }

        }
    }

}

fun main() {
    applicationFor {
        val handler = mockk<CaseControlHandler>(relaxed = true)

        val caseName = "Bondi"
        val id = 45L
        val bondiComment = "Go to Bondi now!"
        val viewableCase = createCaseWithInterpretation(
            name = caseName,
            caseId = id,
            conclusionTexts = listOf(bondiComment)
        )
        val condition = hasCurrentValue(1, Attribute(2, "Surf 1"))
        val suggestedCondition = NonEditableSuggestedCondition(condition)
        CaseControl(
            currentCase = viewableCase,
            conditionHints = listOf(suggestedCondition),
            cornerstoneStatus = CornerstoneStatus(viewableCase, 42, 84),
            handler = handler
        )
    }
}