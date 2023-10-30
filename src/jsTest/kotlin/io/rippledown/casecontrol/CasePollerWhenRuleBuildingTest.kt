package io.rippledown.casecontrol

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.interpretation.clickCancelButton
import io.rippledown.interpretation.requireCancelButtonShowing
import io.rippledown.interpretation.requireDoneButtonShowing
import io.rippledown.interpretation.startToBuildRuleForRow
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import kotlinx.coroutines.MainScope
import mocks.config
import mocks.mock
import proxy.requireNumberOfCases
import proxy.requireNumberOfCasesNotToBeShowing
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CasePollerWhenRuleBuildingTest {

    @Test
    fun shouldNotShowNumberOfCasesWhenRuleBuilding() {
        val caseName = "case"
        val caseId = CaseId(1, caseName)
        val bondiComment = "Go to Bondi now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(bondiComment)))
        val case = createCaseWithInterpretation(
            id = caseId.id,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val config = config {
            returnCasesInfo = CasesInfo(listOf(caseId))
            returnCase = case
        }
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                //Given
                requireCaseToBeShowing(caseName)
                requireNumberOfCases(1)

                //When
                startToBuildRuleForRow(0)

                //Then
                requireNumberOfCasesNotToBeShowing()
            }
        }
    }

    @Test
    fun shouldCallHandlerWhenRuleBuildingIsStarted() {
        val caseName = "case"
        val caseId = CaseId(1, caseName)
        val bondiComment = "Go to Bondi now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(bondiComment)))
        val case = createCaseWithInterpretation(
            id = caseId.id,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        var inProgress = false
        val config = config {
            returnCasesInfo = CasesInfo(listOf(caseId))
            returnCase = case
        }
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { ruleInProgress -> inProgress = ruleInProgress }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                //Given
                requireCaseToBeShowing(caseName)
                inProgress shouldBe false

                //When
                startToBuildRuleForRow(0)

                //Then
                inProgress shouldBe true
            }
        }
    }

    @Test
    fun shouldCallHandlerWhenRuleBuildingIsCancelled() {
        val caseName = "case"
        val caseId = CaseId(1, caseName)
        val bondiComment = "Go to Bondi now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(bondiComment)))
        val case = createCaseWithInterpretation(
            id = caseId.id,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        var inProgress = false
        val config = config {
            returnCasesInfo = CasesInfo(listOf(caseId))
            returnCase = case
        }
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { ruleInProgress -> inProgress = ruleInProgress }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                //Given rule building is in progress
                requireCaseToBeShowing(caseName)
                startToBuildRuleForRow(0)
                inProgress shouldBe true

                //When the rule building session is cancelled
                requireDoneButtonShowing()
                requireCancelButtonShowing()
                clickCancelButton()

                //Then
                inProgress shouldBe false
            }
        }
    }
}

