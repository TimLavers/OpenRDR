package io.rippledown.casecontrol

import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.interpretation.startToBuildRuleForRow
import io.rippledown.kb.requireKBInfoToBeHidden
import io.rippledown.kb.requireKBInfoToBeVisible
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import main.Api
import mocks.config
import mocks.mock
import proxy.requireNumberOfCases
import proxy.requireNumberOfCasesNotToBeShowing
import proxy.waitForNextPoll
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CasePollerWhenRuleBuildingTest {

    @Test
    fun shouldNotShowNumberOfCasesWhenRuleBuilding(): TestResult {
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
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                waitForNextPoll()
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
    fun shouldHideKBInfoWhenRuleBuildingIsStarted(): TestResult {
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
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                waitForNextPoll()
                requireCaseToBeShowing(caseName)
                requireKBInfoToBeVisible()
                //When
                startToBuildRuleForRow(0)

                //Then
                requireKBInfoToBeHidden()
            }
        }
    }
}
