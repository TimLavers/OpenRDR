package io.rippledown.casecontrol

import Api
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseNotToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
import io.rippledown.interpretation.clickDoneButton
import io.rippledown.interpretation.startToBuildRuleForRow
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.MainScope
import mocks.config
import mocks.mock
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseControlRuleBuildingTest {

    @Test
    fun shouldRemoveCornerstoneViewAfterBuildingARule() {
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
        val cornerstoneCase = createCaseWithInterpretation(
            id = 2,
            name = "Bondi"
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = case
            returnCornerstoneStatus = CornerstoneStatus(
                cornerstoneToReview = cornerstoneCase,
                indexOfCornerstoneToReview = 0,
                numberOfCornerstones = 1
            )
        }

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                //Given
                requireCaseToBeShowing(caseName)
                //Build a rule for the Addition
                startToBuildRuleForRow(0)
                requireCornerstoneCaseToBeShowing(cornerstoneCase.name)

                //When
                clickDoneButton()

                //Then
                requireCornerstoneCaseNotToBeShowing()
            }
        }
    }
}