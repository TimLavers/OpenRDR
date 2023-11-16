package io.rippledown.casecontrol

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.cornerstoneview.requireCornerstoneCaseNotToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
import io.rippledown.interpretation.*
import io.rippledown.main.Api
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.diff.*
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import proxy.findAllById
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseControlTest {

    @Test
    fun shouldListCaseNames(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                findAllById(CASE_NAME_PREFIX).length shouldBe 2
                val elementA = findById("$CASE_NAME_PREFIX${caseId1.name}")
                val elementB = findById("$CASE_NAME_PREFIX${caseId2.name}")
                elementA.textContent shouldBe caseA
                elementB.textContent shouldBe caseB
            }
        }
    }

    @Test
    fun shouldSelectACaseIdWhenCaseNameClicked(): TestResult {
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
        val fc = FC {
            CaseControl {
                caseIds = threeCaseIds
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        config.returnCase = createCase(caseB, 2)
        return runReactTest(fc) { container ->
            with(container) {
                selectCaseByName(caseB)
                requireCaseToBeShowing(caseB)
            }
        }
    }

    @Test
    fun shouldShowCaseViewForTheFirstCase(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireCaseToBeShowing(caseName1)
            }
        }
    }

    @Test
    fun shouldShowCaseListForManyCases(): TestResult {

        val caseIds = (1..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        val caseName100 = "case 100"
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(CaseId(100, caseName100))
        }

        val fc = FC {
            CaseControl {
                this.caseIds = caseIds
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                selectCaseByName(caseName100)
                requireCaseToBeShowing(caseName100)
            }
        }
    }

    @Test
    fun shouldNotShowCornerstoneViewIfNoCornerstone(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
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
    }

    @Test
    fun shouldShowConditionHintsWhenRuleSessionIsStarted(): TestResult {
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
        val condition = hasCurrentValue(1, Attribute(2, "surf"))
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
            returnConditionList = ConditionList(listOf(condition))
        }

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                waitForEvents()
                requireCaseToBeShowing(caseName)
                selectChangesTab()
                waitForEvents()
                requireNumberOfRows(2)
                moveMouseOverRow(1)
                waitForEvents()

                //When
                clickBuildIconForRow(1)
                waitForEvents()

                //Then
                waitForEvents()
                waitForEvents()
                waitForEvents()
                requireDoneButtonShowing()
                requireConditions(listOf(condition.asText()))
            }
        }
    }

    @Test
    fun shouldCancelConditionSelector(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
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
    }

    @Test
    fun shouldShowCornerstoneWhenBuildingARule(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
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
    }

    @Test
    fun shouldNotShowCornerstoneAfterBuildingARule(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
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

                //When
                clickDoneButton()
                waitForEvents()

                //Then
                requireCornerstoneCaseNotToBeShowing()
            }
        }
    }

    @Test
    fun shouldNotShowCornerstoneAfterCancellingARuleBuildingSession(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
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

                //When
                clickCancelButton()
                waitForEvents()

                //Then
                requireCornerstoneCaseNotToBeShowing()
            }
        }
    }

    @Test
    fun shouldNotShowCaseSelectorWhenBuildingARule(): TestResult {
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

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { _ -> }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                waitForEvents()
                requireCaseSelectorToBeShowing()
                requireCaseToBeShowing(caseName)
                //start to build a rule for the Addition
                selectChangesTab()
                waitForEvents()
                requireNumberOfRows(1)
                moveMouseOverRow(0)
                waitForEvents()

                //When
                clickBuildIconForRow(0)
                waitForEvents()

                //Then
                waitForEvents()
                waitForEvents()
                waitForEvents()
                requireCaseSelectorNotToBeShowing()
            }
        }
    }

    @Test
    fun shoulCallHandlerMethodWhenStartingToBuildARule(): TestResult {
        val caseId = 1L
        val caseName = "Manly"
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
        val config = config {
            expectedCaseId = caseId
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
            returnCornerstoneStatus = CornerstoneStatus()
        }
        var isInProgress = false

        val fc = FC {
            CaseControl {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = MainScope()
                ruleSessionInProgress = { inProgress ->
                    isInProgress = inProgress
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                isInProgress shouldBe false
                requireCaseSelectorToBeShowing()
                requireCaseToBeShowing(caseName)
                //start to build a rule for the Addition
                selectChangesTab()
                requireNumberOfRows(1)
                moveMouseOverRow(0)

                //When
                clickBuildIconForRow(0)
                waitForEvents()

                //Then
                isInProgress shouldBe true
            }
        }
    }
}