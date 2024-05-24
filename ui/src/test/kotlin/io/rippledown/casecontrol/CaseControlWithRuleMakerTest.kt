package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.diffview.clickBuildIconForRow
import io.rippledown.diffview.requireNumberOfDiffRows
import io.rippledown.interpretation.selectDifferencesTab
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.rule.clickCancelRuleButton
import io.rippledown.rule.clickFinishRuleButton
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseControlWithRuleMakerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseControlHandler
    lateinit var condition: Condition

    val caseName = "Bondi"
    val caseId = CaseId(45L, caseName)
    val beachComment = "Enjoy the beach!"
    val bondiComment = "Go to Bondi now!"
    val diffList = DiffList(
        listOf(
            Unchanged(beachComment),
            Addition(bondiComment),
        )
    )
    val viewableCase = createCaseWithInterpretation(
        name = caseName,
        id = 1,
        conclusionTexts = listOf(bondiComment),
        diffs = diffList
    )

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>(relaxed = true)


        condition = hasCurrentValue(1, Attribute(2, "surf"))
        coEvery { handler.buildRule(any()) } returns viewableCase
        coEvery { handler.getCase(any()) } returns viewableCase
        coEvery { handler.saveCase(any()) } answers { firstArg() }
        coEvery { handler.conditionHintsForCase(any()) } returns listOf(condition)
    }

    @Test
    fun `should call handler to set the rule in progress flag when a rule session is started`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(false, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            selectDifferencesTab()
            requireNumberOfDiffRows(2)

            //When
            clickBuildIconForRow(1)

            //Then
            coVerify { handler.setRuleInProgress(true) }
        }
    }

    @Test
    fun `should call handler to start a backend rule session`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(false, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            selectDifferencesTab()
            requireNumberOfDiffRows(2)

            //When
            clickBuildIconForRow(1)

            //Then
            coVerify { handler.startRuleSession(SessionStartRequest(caseId.id!!, diffList[1])) }
        }
    }

    @Test
    fun `should call handler to finish rule session`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(true, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)

            //When
            clickFinishRuleButton()

            //Then
            coVerify { handler.setRuleInProgress(false) }
        }
    }

    @Test
    fun `should call handler to build a rule with the appropriate rule request`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(true, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)

            //When
            clickFinishRuleButton()

            //Then
            val expectedRuleRequest = RuleRequest(1, ConditionList())
            coVerify { handler.buildRule(expectedRuleRequest) }
        }
    }

    @Test
    fun `should call handler when a rule session is cancelled`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(true, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)

            //When
            clickCancelRuleButton()

            //Then
            coVerify { handler.setRuleInProgress(false) }
        }
    }

    @Test
    fun `should not show case selector when a rule session is started`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(true, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)

            //Then
            requireCaseSelectorNotToBeDisplayed()
        }
    }

    @Test
    fun `should call handler to set rule in progress when a rule session is started`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseControl(false, CasesInfo(listOf(caseId)), handler)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            selectDifferencesTab()
            requireNumberOfDiffRows(2)

            //When
            clickBuildIconForRow(1)

            //Then
            coVerify { handler.setRuleInProgress(true) }
        }
    }


    /*
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
    */

    /*


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