package io.rippledown.caselist

import Api
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.interpretation.*
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class ConditionSelectionWhenShowingCornerstoneTest {

    @Test
    fun shouldUpdateCornerstoneStatusWhenAConditionIsSelected() = runTest {
        val caseId = 1L
        val caseName = "Manly"
        val caseIdList = listOf(CaseId(caseId, caseName))
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(beachComment)))
        val case = createCaseWithInterpretation(
            id = caseId,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val cornerstone = createCaseWithInterpretation(
            id = 0L,
            name = "Bondi",
        )
        val condition1 = hasCurrentValue(1, Attribute(1, "surf"))
        val condition2 = hasCurrentValue(2, Attribute(2, "sand"))

        val config = config {
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = case
            returnConditionList = ConditionList(listOf(condition1, condition2))
            returnCornerstoneStatus = CornerstoneStatus(cornerstone, 0, 1)
        }

        val fc = FC {
            CaseList {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(fc)) {
            waitForEvents()
            requireCaseToBeShowing(caseName)
            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(1)
            moveMouseOverRow(0)
            waitForEvents()
            clickBuildIconForRow(0)
            requireConditionsToBeNotSelected(listOf(condition1.asText(), condition2.asText()))

            clickConditionWithIndex(0)
            waitForEvents()
            requireConditionsToBeSelected(listOf(condition1.asText()))
            requireConditionsToBeNotSelected(listOf(condition2.asText()))

            //deselect
            clickConditionWithIndex(0)
            waitForEvents()
            requireConditionsToBeNotSelected(listOf(condition1.asText(), condition2.asText()))
        }
    }


}