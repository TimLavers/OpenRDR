package io.rippledown.caselist

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseNotToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
import io.rippledown.interpretation.*
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.HasCurrentValue
import io.rippledown.model.diff.*
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.findAllById
import proxy.findById
import proxy.waitForEvents
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CaseListTest {

    @Test
    fun shouldListCaseNames() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val twoCaseIds = listOf(
            caseId1, caseId2
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase(caseId1)
        }

        val vfc = VFC {
            CaseList {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }

        checkContainer(vfc) { container ->
            with(container) {
                findAllById(CASE_ID_PREFIX).length shouldBe 2
                val elementA = findById("$CASE_ID_PREFIX${caseId1.id}")
                val elementB = findById("$CASE_ID_PREFIX${caseId2.id}")
                elementA.textContent shouldBe caseA
                elementB.textContent shouldBe caseB
            }
        }
    }

    @Test
    fun shouldSelectACaseIdWhenCaseNameClicked() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val config = config {
            returnCasesInfo = CasesInfo(threeCaseIds)
            returnCase = createCase(caseId1)
        }
        val vfc = VFC {
            CaseList {
                caseIds = threeCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        config.returnCase = createCase(caseB, 2)
        val container = createRootFor(vfc)
        with(container) {
            selectCaseById(2)
            requireCaseToBeShowing(caseB)
        }
    }

    @Test
    fun shouldShowCaseViewForTheFirstCase() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseId1 = CaseId(1, caseName1)
        val caseId2 = CaseId(2, caseName2)
        val twoCaseIds = listOf(
            caseId1, caseId2
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase(caseId1)
        }

        val vfc = VFC {
            CaseList {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireCaseToBeShowing(caseName1)
            }
        }
    }

    @Test
    fun shouldShowCaseListForManyCases() = runTest {

        val caseIds = (1..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(CaseId(100, "case 100"))
        }

        val vfc = VFC {
            CaseList {
                this.caseIds = caseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            selectCaseById(100)
            requireCaseToBeShowing("case 100")

        }
    }

    @Test
    fun shouldShowConditionSelector() = runTest {
        val caseName = "Bondi"
        val caseId = 45L
        val caseIdList = listOf(CaseId(caseId, caseName))
        val bondiComment = "Go to Bondi now!"
        val manlyComment = "Go to Manly now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(
            listOf(
                Unchanged(beachComment),
                Removal(manlyComment),
                Addition(bondiComment),
                Replacement(manlyComment, bondiComment)
            )
        )
        val caseWithInterp = createCaseWithInterpretation(
            name = caseName,
            id = caseId,
            conclusionTexts = listOf(beachComment, manlyComment, bondiComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
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
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            waitForEvents()
            requireDoneButtonNotShowing()
            clickBuildIconForRow(2)
            requireDoneButtonShowing()
        }
    }

    @Test
    fun shouldNotShowCornerstoneViewIfNoCornerstone() = runTest {
        val id = 1L
        val caseName = "Bondi"
        val caseIdList = listOf(CaseId(id, caseName))
        val bondiComment = "Go to Bondi now!"
        val diffList = DiffList(
            listOf(
                Addition(bondiComment),
            )
        )
        val caseWithInterp = createCaseWithInterpretation(
            id = id,
            name = caseName,
            diffs = diffList
        )
        val config = config {
            expectedCaseId = id
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
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
            requireCornerstoneCaseNotToBeShowing()
        }
    }

    @Test
    fun shouldUpdateConditionHintsWhenRuleIsStarted() = runTest {
        val caseId = 45L
        val caseName = "Bondi"
        val caseIdList = listOf(CaseId(caseId, caseName))
        val beachComment = "Enjoy the beach!"
        val bondiComment = "Go to Bondi now!"
        val diffList = DiffList(
            listOf(
                Unchanged(beachComment),
                Addition(bondiComment),
            )
        )
        val caseWithInterp = createCaseWithInterpretation(
            name = caseName,
            id = caseId,
            conclusionTexts = listOf(beachComment, bondiComment),
            diffs = diffList
        )
        val condition = HasCurrentValue(1, Attribute(2, "surf"))
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
            returnConditionList = ConditionList(listOf(condition))
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
            requireNumberOfRows(2)
            moveMouseOverRow(1)
            waitForEvents()
            requireDoneButtonNotShowing()
            clickBuildIconForRow(1)
            requireDoneButtonShowing()
            requireConditions(listOf(condition.asText()))
        }
    }

    @Test
    fun shouldCancelConditionSelector() = runTest {
        val caseName = "Bondi"
        val caseId = 45L
        val caseIdList = listOf(CaseId(caseId, caseName))
        val bondiComment = "Go to Bondi now!"
        val manlyComment = "Go to Manly now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(
            listOf(
                Unchanged(beachComment),
                Removal(manlyComment),
                Addition(bondiComment),
                Replacement(manlyComment, bondiComment)
            )
        )
        val caseWithInterp = createCaseWithInterpretation(
            name = caseName,
            id = caseId,
            conclusionTexts = listOf(beachComment, manlyComment, bondiComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
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
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            waitForEvents()
            clickBuildIconForRow(2)
            requireCancelButtonShowing()
            //cancel the condition selector
            clickCancelButton()
            waitForEvents()
            requireDoneButtonNotShowing()
        }
    }

    @Test
    fun shouldShowCornerstoneWhenBuildingARule() = runTest {
        val caseId = 1L
        val cornerstoneId = 2L
        val caseName = "Manly"
        val cornerstoneCaseName = "Bondi"
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
        val cornerstoneCase = createCaseWithInterpretation(
            id = cornerstoneId,
            name = cornerstoneCaseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
            returnCornerstoneStatus = CornerstoneStatus(cornerstoneCase, 42, 84)
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
            requireCornerstoneCaseToBeShowing(cornerstoneCaseName)
        }
    }

    @Test
    fun shouldNotShowCaseSelectorWhenBuildingARule() = runTest {
        val caseId = 1L
        val cornerstoneId = 2L
        val caseName = "Manly"
        val cornerstoneCaseName = "Bondi"
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
        val cornerstoneCase = createCaseWithInterpretation(
            id = cornerstoneId,
            name = cornerstoneCaseName,
            conclusionTexts = listOf(beachComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
            returnCornerstoneStatus = CornerstoneStatus(cornerstoneCase, 42, 84)
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
            requireCaseSelectorToBeShowing()
            requireCaseToBeShowing(caseName)
            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(1)
            moveMouseOverRow(0)
            waitForEvents()
            clickBuildIconForRow(0)
            waitForEvents()
            requireCaseSelectorNotToBeShowing()
        }
    }
}