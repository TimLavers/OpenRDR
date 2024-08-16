package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.diffview.clickBuildIconForRow
import io.rippledown.diffview.requireNumberOfDiffRows
import io.rippledown.interpretation.selectDifferencesTab
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.edit.FixedSuggestedCondition
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
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
    val caseId = CaseId(id, caseName)
    val beachComment = "Enjoy the beach!"
    val bondiComment = "Go to Bondi now!"
    val diffList = DiffList(
        listOf(
            Unchanged(beachComment),
            Addition(bondiComment),
        )
    )
    val condition = hasCurrentValue(1, Attribute(2, "surf"))
    val suggestedCondition = FixedSuggestedCondition(condition)
    val viewableCase = createCaseWithInterpretation(
        name = caseName,
        id = id,
        conclusionTexts = listOf(bondiComment),
        diffs = diffList
    )

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>(relaxed = true)
        coEvery { handler.selectCornerstone(any()) } returns viewableCase
    }

    @Test
    fun `should call handler to start a backend rule session`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            //Given
            waitForCaseToBeShowing(caseName)
            selectDifferencesTab()
            requireNumberOfDiffRows(2)

            //When
            clickBuildIconForRow(1)

            //Then
            coVerify { handler.startRuleSession(SessionStartRequest(caseId.id!!, diffList[1])) }
        }
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
            verify { handler.setInfoMessage(NO_CORNERSTONES_TO_REVIEW_MSG) }
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
            selectDifferencesTab()
            requireNumberOfDiffRows(2)

            //When
            clickBuildIconForRow(1)

            //Then
            verify { handler.setInfoMessage("") }
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
            waitForIdle()

            //When
            clickAvailableConditionWithText(condition.asText())
            waitForIdle()

            //Then
            val slot = slot<UpdateCornerstoneRequest>()
            verify { handler.updateCornerstoneStatus(capture(slot)) }
            slot.captured.conditionList shouldBe ConditionList(listOf(suggestedCondition))
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
            capturedRequests[0].conditionList shouldBe ConditionList(listOf(suggestedCondition))
            capturedRequests[1].conditionList shouldBe ConditionList(listOf())
        }
    }
}