package io.rippledown.casecontrol

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.interpretation.startToBuildRuleForRow
import io.rippledown.main.Api
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import proxy.waitForNextPoll
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CasePollerWhenRuleBuildingTest {

    @Test
    fun shouldCallRuleInProgressWhenRuleBuildingIsStarted(): TestResult {
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
        var isRuleSessionInProgress = false
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
                ruleInProgress = { inProgress ->
                    isRuleSessionInProgress = inProgress
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                waitForNextPoll()
                requireCaseToBeShowing(caseName)
                isRuleSessionInProgress shouldBe false

                //When
                startToBuildRuleForRow(0)

                //Then
                isRuleSessionInProgress shouldBe true
            }
        }
    }
}
