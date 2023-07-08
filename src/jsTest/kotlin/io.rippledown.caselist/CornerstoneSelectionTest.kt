package io.rippledown.caselist

import Api
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
import io.rippledown.cornerstoneview.selectNextCornerstone
import io.rippledown.cornerstoneview.selectPreviousCornerstone
import io.rippledown.interpretation.clickBuildIconForRow
import io.rippledown.interpretation.moveMouseOverRow
import io.rippledown.interpretation.requireNumberOfRows
import io.rippledown.interpretation.selectChangesTab
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

class CornerstoneSelectionTest {


    @Test
    fun shouldSelectNextAndPreviousCornerstone() = runTest {
        val caseId = 1L
        val caseName = "Manly"
        val cornerstoneName = "Bondi"
        val caseIdList = listOf(CaseId(caseId, caseName))
        val bondiComment = "Go to Bondi now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(listOf(Addition(bondiComment)))
        val caseWithInterp = createCaseWithInterpretation(
            id = caseId,
            name = caseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val numberOfCornerstones = 30
        val cornerstones = (1..numberOfCornerstones).map { i ->
            createCaseWithInterpretation(
                id = i.toLong(),
                name = "$cornerstoneName$i",
                conclusionTexts = listOf(beachComment),
            )
        }
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
            returnCornerstoneStatus = CornerstoneStatus(cornerstones[0], 0, numberOfCornerstones)
        }

        val vfc = VFC {
            CaseList {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForEvents()
            requireCaseToBeShowing(caseName)
            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(1)
            moveMouseOverRow(0)
            waitForEvents()
            clickBuildIconForRow(0)
            requireCornerstoneCaseToBeShowing(cornerstones[0].name)

            config.returnCornerstoneStatus = CornerstoneStatus(cornerstones[1], 1, numberOfCornerstones)
            selectNextCornerstone()
            requireCornerstoneCaseToBeShowing(cornerstones[1].name)

            config.returnCornerstoneStatus = CornerstoneStatus(cornerstones[0], 0, numberOfCornerstones)
            selectPreviousCornerstone()
            requireCornerstoneCaseToBeShowing(cornerstones[0].name)
        }
    }


}