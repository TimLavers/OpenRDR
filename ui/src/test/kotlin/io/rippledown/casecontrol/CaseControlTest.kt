package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.interpretation.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Replacement
import io.rippledown.model.interpretationview.ViewableInterpretation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import kotlin.test.Test

class CaseControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseControlHandler

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>(relaxed = true)
    }

    @Test
    fun `should list case names`() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val twoCaseIds = listOf(
            caseId1, caseId2
        )
        val case = createCase(caseId1)
        coEvery { handler.getCase(1) } returns case
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(twoCaseIds), handler)
            }
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
        }
    }

    @Test
    fun `should show a case when its case name is clicked`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseNameC = "case C"
        val caseId1 = CaseId(id = 1, name = caseNameA)
        val caseId2 = CaseId(id = 2, name = caseNameB)
        val caseId3 = CaseId(id = 3, name = caseNameC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val caseA = createCase(caseId1)
        val caseB = createCase(caseId2)

        coEvery { handler.getCase(1) } returns caseA
        coEvery { handler.getCase(2) } returns caseB
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(threeCaseIds), handler)
            }
            //Given
            requireNumberOfCasesOnCaseList(3)
            requireNamesToBeShowingOnCaseList(caseNameA, caseNameB, caseNameC)

            //When
            selectCaseByName(caseNameB)

            //Then
            waitForCaseToBeShowing(caseNameB)

        }
    }

    @Test
    fun `should show the interpretation of the first case`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { handler.getCase(1) } returns case
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(caseIds), handler)
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)

            //When
            waitForCaseToBeShowing(caseA)

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should save the case when its interpretation changes`() = runTest {
        val caseA = "case A"
        val caseId = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId)
        val bondiComment = "Go to Bondi"
        val manlyComment = "Go to Manly"
        val originalCase = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { handler.getCase(1) } returns originalCase
        val slot = slot<ViewableCase>()
        coEvery { handler.saveCase(capture(slot)) } answers { firstArg() }
        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(caseIds), handler)
            }
            //Given
            requireNumberOfCasesOnCaseList(1)
            requireNamesToBeShowingOnCaseList(caseA)
            waitForCaseToBeShowing(caseA)
            requireInterpretation(bondiComment)
            waitForIdle()

            //When
            replaceInterpretationBy(manlyComment)
            waitForIdle()

            //Then
            slot.captured.viewableInterpretation.verifiedText shouldBe manlyComment
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Ignore("TODO: Fix this test")
    fun `should debounce the saving the case when its interpretation changes`() = runTest {
        val caseA = "case A"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val bronteComment = "Go to Bronte"
        val originalCase = createCaseWithInterpretation(caseA, 1)
        coEvery { handler.getCase(1) } returns originalCase

        val slot = slot<ViewableCase>()
        coEvery { handler.saveCase(capture(slot)) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(caseIds), handler)
            }
            //Given
            requireNumberOfCasesOnCaseList(1)
            waitForCaseToBeShowing(caseA)
            requireInterpretation("")

            //When
            enterInterpretation(bondiComment)
            replaceInterpretationBy(bronteComment)
            delay(2 * DEBOUNCE_WAIT_PERIOD_MILLIS)
            waitForIdle()

            //Then only the last change is saved
            coVerify(exactly = 1) { handler.saveCase(any()) }
            slot.captured.viewableInterpretation!!.verifiedText shouldBe bronteComment
        }
    }

    @Test
    fun `should update the badge count when the interpretation changes`() = runTest {
        val caseA = "case A"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val manlyComment = "Go to Manly"
        val originalCase = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        val updatedCase = ViewableCase(
            case = originalCase.case,
            viewableInterpretation = ViewableInterpretation(originalCase.case.interpretation).apply {
                verifiedText = manlyComment
                diffList = DiffList(listOf(Replacement(bondiComment, manlyComment)))
            },
            viewProperties = originalCase.viewProperties
        )
        coEvery { handler.getCase(1) } returns originalCase
        coEvery { handler.saveCase(any()) } returns updatedCase

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(caseIds), handler)
            }
            //Given
            requireInterpretation(bondiComment)
            requireBadgeOnDifferencesTabNotToBeShowing()

            //When
            replaceInterpretationBy(manlyComment)
            waitForIdle()

            //Then the badge count should indicate 1 change (i.e. replacement)
            requireBadgeOnDifferencesTabToShow(1)
        }
    }


    @Test
    fun `should update the interpretation when a case is selected`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val malabarComment = "Go to Malabar"

        val viewableCaseA = createCaseWithInterpretation(
            name = caseA,
            id = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createCaseWithInterpretation(
            name = caseB,
            id = 2,
            conclusionTexts = listOf(malabarComment)
        )
        coEvery { handler.getCase(caseId1.id!!) } returns viewableCaseA
        coEvery { handler.getCase(caseId2.id!!) } returns viewableCaseB
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(caseIds), handler)
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
            waitForCaseToBeShowing(caseA)
            requireInterpretation(bondiComment)

            //When
            selectCaseByName(caseB)

            //Then
            waitForCaseToBeShowing(caseB)
            requireInterpretation(malabarComment)
        }
    }

    @Test
    fun `should show case view for the first case`() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseId1 = CaseId(1, caseName1)
        val caseId2 = CaseId(2, caseName2)
        val twoCaseIds = listOf(
            caseId1, caseId2
        )
        coEvery { handler.getCase(1) } returns createCaseWithInterpretation(
            name = caseName1,
            id = 1
        )
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(twoCaseIds), handler)
            }
            waitForCaseToBeShowing(caseName1)
        }
    }

    @Test
    fun `should show case list for several cases`() = runTest {

        val caseIds = (1..10).map { i ->
            val caseId = CaseId(id = i.toLong(), name = "case $i")
            coEvery { handler.getCase(caseId.id!!) } returns createCase(caseId)
            caseId
        }
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        val caseName1 = "case 1"
        val caseName10 = "case 10"
        with(composeTestRule) {
            setContent {
                CaseControl(CasesInfo(caseIds), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName1)

            //When
            selectCaseByName(caseName10)

            //Then
            waitForCaseToBeShowing(caseName10)
        }
    }

    /*
@Test
    fun diffPanelShouldUpdateWhenTheInterpretationIsEdited(): TestResult {
        val addedText = "Bring your flippers!"
        val diffListToReturn = DiffList(
            listOf(
                Addition(addedText),
            )
        )
        val interpretationWithDiffs = ViewableInterpretation().apply { diffList = diffListToReturn }
        val config = config {
            returnInterpretationAfterSavingInterpretation = interpretationWithDiffs
        }

        val fc = FC {
            InterpretationTabs {
                scope = MainScope()
                api = Api(mock(config))
                interpretation = ViewableInterpretation()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireInterpretation("")
                enterInterpretation(addedText)
                waitForDebounce()
                selectChangesTab()
                requireNumberOfRows(1)
                requireChangedTextInRow(0, addedText)
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

     */
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val handler = mockk<CaseControlHandler>(relaxed = true)
            val caseNames = (0..100).map { "case $it" }
            val caseIds = caseNames.mapIndexed { index, name ->
                CaseId(id = index.toLong(), name = name)
            }
            caseIds.map { caseId ->
                createCaseWithInterpretation(caseId.name, caseId.id, listOf("Go to Bondi $caseId"))
            }.forEach {
                coEvery { handler.getCase(it.case.caseId.id!!) } returns it
            }
            CaseControl(CasesInfo(caseIds), handler)
        }
    }
}