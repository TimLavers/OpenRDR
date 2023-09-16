package io.rippledown.caselist

import Api
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseNotToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
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
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class CornerstoneUpdateTest {

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
        val currentCCStatus = CornerstoneStatus(cornerstone, 0, 1)
        val condition = hasCurrentValue(1, Attribute(2, "surf"))
        val updateCornerstoneRequest = UpdateCornerstoneRequest(
            cornerstoneStatus = currentCCStatus,
            conditionList = ConditionList(listOf(condition))
        )

        val config = config {
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = case
            returnConditionList = ConditionList(listOf(condition))
            returnCornerstoneStatus = CornerstoneStatus(cornerstone, 0, 1)
            expectedUpdateCornerstoneRequest = updateCornerstoneRequest
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
            requireCornerstoneCaseToBeShowing(cornerstone.name)

            config.returnCornerstoneStatus = CornerstoneStatus()
            clickConditionWithIndex(0)
            waitForEvents()
            requireCornerstoneCaseNotToBeShowing()
        }
    }


}