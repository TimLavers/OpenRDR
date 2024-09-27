package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.model.Attribute
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.rule.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseControlWithRuleMakerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseControlHandler

    val caseName = "Bondi"
    val id = 45L
    val bondiComment = "Go to Bondi now!"
    val condition = hasCurrentValue(1, Attribute(2, "surf"))
    val suggestedCondition = NonEditableSuggestedCondition(condition)
    val viewableCase = createCaseWithInterpretation(
        name = caseName,
        id = id,
        conclusionTexts = listOf(bondiComment)
    )
    val cornerstoneStatus = CornerstoneStatus(viewableCase, 42, 84)

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>(relaxed = true)
//        coEvery { handler.selectCornerstone(any()) } returns cornerstoneStatus
    }

    @Test
    fun `should call handler to build a rule with the appropriate rule request`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    cornerstoneStatus = CornerstoneStatus(),
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            //Given
            waitForCaseToBeShowing(caseName)

            //When
            clickFinishRuleButton()

            //Then
            val expectedRuleRequest = RuleRequest(id, RuleConditionList())
            coVerify { handler.buildRule(expectedRuleRequest) }
        }
    }

    @Test
    fun `should call handler when a rule session is cancelled`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    cornerstoneStatus = CornerstoneStatus(),
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            //Given
            waitForCaseToBeShowing(caseName)

            //When
            clickCancelRuleButton()

            //Then
            coVerify { handler.endRuleSession() }
        }
    }

    @Test
    fun `should set the 'no cornerstones to review' message when there are no cornerstones`() {
        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    cornerstoneStatus = CornerstoneStatus(),
                    conditionHints = listOf(),
                    handler = handler
                )

            }
            verify { handler.setRightInfoMessage(NO_CORNERSTONES_TO_REVIEW_MSG) }
        }
    }

    @Test
    fun `should remove the 'no cornerstones to review' message when there are cornerstones`() {
        val ccStatus = CornerstoneStatus(viewableCase, 42, 84)

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    cornerstoneStatus = ccStatus,
                    conditionHints = listOf(),
                    handler = handler
                )

            }
            //Given
            waitForCaseToBeShowing(caseName)

            //Then
            verify { handler.setRightInfoMessage("") }
        }
    }

    @Test
    fun `should call handler to update the cornerstone status when a condition is added to the rule`() {
        val ccStatus = CornerstoneStatus(viewableCase, 42, 84)

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    cornerstoneStatus = ccStatus,
                    conditionHints = listOf(suggestedCondition),
                    handler = handler
                )
            }
            //Given
            waitForCaseToBeShowing(caseName)
            requireRuleMakerToBeDisplayed()
            requireAvailableConditionsToBeDisplayed(listOf(condition.asText()))
//            waitForIdle()

            //When
            clickAvailableConditionWithText(condition.asText())
//            waitForIdle()

            //Then
//            val slot = slot<UpdateCornerstoneRequest>()
//            verify { handler.updateCornerstoneStatus(capture(slot)) }
//            slot.captured.conditionList shouldBe RuleConditionList(listOf(suggestedCondition.initialSuggestion))
        }
    }

    @Test
    fun `should call handler to update the cornerstone status when a condition is removed from the rule`() {
        val ccStatus = CornerstoneStatus(viewableCase, 42, 84)

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    cornerstoneStatus = ccStatus,
                    conditionHints = listOf(suggestedCondition),
                    handler = handler
                )
            }
            //Given
            waitForCaseToBeShowing(caseName)
            requireAvailableConditionsToBeDisplayed(listOf(condition.asText()))
            waitForIdle()
            clickAvailableConditionWithText(condition.asText())
            requireNoAvailableConditionsToBeDisplayed()

            //When
            clickSelectedConditionWithText(condition.asText())

            //Then
            val capturedRequests = mutableListOf<UpdateCornerstoneRequest>()
            verify { handler.updateCornerstoneStatus(capture(capturedRequests)) }
            capturedRequests.size shouldBe 2
            capturedRequests[0].conditionList shouldBe RuleConditionList(listOf(suggestedCondition.initialSuggestion))
            capturedRequests[1].conditionList shouldBe RuleConditionList(listOf())
        }
    }
}