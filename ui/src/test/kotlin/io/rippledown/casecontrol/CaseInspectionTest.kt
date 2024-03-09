package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.createCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseInspectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseInspectionHandler

    @Before
    fun setUp() {
        handler = mockk<CaseInspectionHandler>()
    }
    @Test
    fun shouldShowCaseView() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val currentCase = createCase(caseId)

        with(composeTestRule) {
            setContent {
                CaseInspection(handler)
            }
            waitForCaseToBeShowing(caseName)
        }
    }

    @Test
    fun shouldShowInterpretation() = runTest {
        val text = "Go to Bondi now!"
        val currentCase = createCase(name = "case a", id = 1L)
        currentCase.viewableInterpretation.textGivenByRules = text

        with(composeTestRule) {
            setContent {
                CaseInspection(handler)
            }
            requireInterpretation(text)
        }
    }

    /*

        @Test
        fun shouldCallRuleSessionInProgressWhenRuleIsStarted(): TestResult {
            val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
            val currentCase = createCaseWithInterpretation(
                name = "Bondi",
                id = 45L,
                diffs = diffList
            )
            var inProgress = false
            val fc = FC {
                CaseInspection {
                    case = currentCase
                    api = Api(mock(config {}))
                    scope = MainScope()
                    ruleSessionInProgress = { started ->
                        inProgress = started
                    }
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing("Bondi")
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)
                    inProgress shouldBe false

                    //When
                    clickBuildIconForRow(0)

                    //Then
                    inProgress shouldBe true
                }
            }
        }

        @Test
        fun shouldShowConditionSelectorWhenRuleIsStarted(): TestResult {
            val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
            val currentCase = createCaseWithInterpretation(
                name = "Bondi",
                id = 45L,
                diffs = diffList
            )
            val fc = FC {
                CaseInspection {
                    case = currentCase
                    ruleSessionInProgress = { _ -> }
                    api = Api(mock(config {}))
                    scope = MainScope()
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing("Bondi")
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)
                    requireDoneButtonNotShowing()

                    //When
                    clickBuildIconForRow(0)

                    //Then
                    requireDoneButtonShowing()
                }
            }
        }

        @Test
        fun shouldCallConditionHintsApiWhenRuleIsStarted(): TestResult {
            val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
            val currentCase = createCaseWithInterpretation(
                name = "Bondi",
                id = 45L,
                diffs = diffList
            )
            val config = config {
                expectedCaseId = currentCase.id
            }
            val fc = FC {
                CaseInspection {
                    case = currentCase
                    ruleSessionInProgress = { _ -> }
                    api = Api(mock(config))
                    scope = MainScope()
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing("Bondi")
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)

                    //When
                    clickBuildIconForRow(0)

                    //Then
                    //Assertion for expected case id is in the mock config
                }
            }
        }

        @Test
        fun shouldCallStartRuleSessionApiWhenRuleIsStarted(): TestResult {
            val caseId = 45L
            val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
            val currentCase = createCaseWithInterpretation(
                name = "Bondi",
                id = caseId,
                diffs = diffList
            )
            val config = config {
                expectedCaseId = currentCase.id
                expectedSessionStartRequest = SessionStartRequest(caseId, diffList.diffs[0])
            }
            val fc = FC {
                CaseInspection {
                    case = currentCase
                    ruleSessionInProgress = { _ -> }
                    api = Api(mock(config))
                    scope = MainScope()
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing("Bondi")
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)

                    //When
                    clickBuildIconForRow(0)

                    //Then
                    //Assertion for expected SessionStartRequest is in the mock config
                }
            }
        }

        @Test
        fun shouldShowCornerstoneWhenRuleSessionIsStarted(): TestResult {
            val caseId = 1L
            val cornerstoneId = 2L
            val caseName = "Manly"
            val cornerstoneCaseName = "Bondi"
            val bondiComment = "Go to Bondi now!"
            val beachComment = "Enjoy the beach!"
            val diffList = DiffList(listOf(Addition(bondiComment)))
            val currentCase = createCaseWithInterpretation(
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
                expectedSessionStartRequest = SessionStartRequest(caseId, diffList.diffs[0])
                returnCornerstoneStatus = CornerstoneStatus(cornerstoneCase, 42, 84)
            }

            val fc = FC {
                CaseInspection {
                    case = currentCase
                    api = Api(mock(config))
                    scope = MainScope()
                    ruleSessionInProgress = { _ -> }
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing(caseName)
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)

                    //When
                    clickBuildIconForRow(0)

                    //Then
                    requireCornerstoneCaseToBeShowing(cornerstoneCaseName)
                }
            }
        }

        @Test
        fun shouldNotShowCornerstoneWhenRuleSessionIsFinished(): TestResult {
            val caseId = 1L
            val cornerstoneId = 2L
            val caseName = "Manly"
            val cornerstoneCaseName = "Bondi"
            val bondiComment = "Go to Bondi now!"
            val beachComment = "Enjoy the beach!"
            val diffList = DiffList(listOf(Addition(bondiComment)))
            val currentCase = createCaseWithInterpretation(
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
                expectedSessionStartRequest = SessionStartRequest(caseId, diffList.diffs[0])
                returnCornerstoneStatus = CornerstoneStatus(cornerstoneCase, 42, 84)
            }

            val fc = FC {
                CaseInspection {
                    case = currentCase
                    api = Api(mock(config))
                    scope = MainScope()
                    ruleSessionInProgress = { _ -> }
                    updateCase = { _ -> }
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing(caseName)
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)
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
        fun shouldCallBuildRuleWhenDoneButtonIsClickedOnConditionSelector(): TestResult {
            val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
            val currentCase = createCaseWithInterpretation(
                name = "Bondi",
                id = 45L,
                diffs = diffList
            )
            val conditionList = ConditionList(
                listOf(
                    hasCurrentValue(1, Attribute(1, "sun")),
                    hasCurrentValue(2, Attribute(2, "surf"))
                )
            )

            val config = config {
                expectedRuleRequest = RuleRequest(
                    caseId = currentCase.id!!,
                    conditions = conditionList
                )
                returnConditionList = conditionList
            }

            val fc = FC {
                CaseInspection {
                    case = currentCase
                    ruleSessionInProgress = { _ -> }
                    api = Api(mock(config))
                    scope = MainScope()
                }
            }
            return runReactTest(fc) { container ->
                with(container) {
                    //Given
                    requireCaseToBeShowing("Bondi")
                    selectChangesTab()
                    requireNumberOfRows(1)
                    moveMouseOverRow(0)
                    clickBuildIconForRow(0)
                    requireDoneButtonShowing()

                    //When
                    clickConditionWithIndex(0)
                    clickConditionWithIndex(1)
                    clickDoneButton()
                    waitForEvents()

                    //Then
                    //Assertion for expected RuleRequest is in the mock config
                }
            }
        }*/
}