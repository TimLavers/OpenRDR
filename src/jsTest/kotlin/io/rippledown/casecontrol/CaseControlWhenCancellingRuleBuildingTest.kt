package io.rippledown.casecontrol

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.interpretation.clickCancelButton
import io.rippledown.interpretation.clickDoneButton
import io.rippledown.interpretation.startToBuildRuleForRow
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
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseControlWhenCancellingRuleBuildingTest {

    @Test
    fun shouldCallHandlerWhenCancellingARule(): TestResult {
        val caseId = 1L
        val caseName = "Manly"
        val caseIdList = listOf(CaseId(caseId, caseName))
        val bondiComment = "Go to Bondi now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(bondiComment)))
        val case = createCaseWithInterpretation(
            id = caseId,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = case
        }
        var inProgress = false
        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { sessionInProgress -> inProgress = sessionInProgress }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                requireCaseToBeShowing(caseName)
                startToBuildRuleForRow(0)
                inProgress shouldBe true

                //When
                clickCancelButton()

                //Then
                inProgress shouldBe false
            }
        }
    }

    @Test
    fun shouldCallHandlerAfterBuildingARule(): TestResult {
        val caseId = 1L
        val caseName = "Manly"
        val caseIdList = listOf(CaseId(caseId, caseName))
        val bondiComment = "Go to Bondi now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(bondiComment)))
        val case = createCaseWithInterpretation(
            id = caseId,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = case
        }
        var inProgress = false
        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { sessionInProgress -> inProgress = sessionInProgress }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                requireCaseToBeShowing(caseName)
                startToBuildRuleForRow(0)
                inProgress shouldBe true

                //When
                clickDoneButton()
                waitForEvents()

                //Then
                inProgress shouldBe false
            }
        }
    }
}