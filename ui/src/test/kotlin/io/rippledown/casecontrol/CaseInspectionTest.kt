package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.diffview.clickBuildIconForRow
import io.rippledown.diffview.requireNumberOfDiffRows
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.interpretation.selectDifferencesTab
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.DiffList
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
        handler = mockk<CaseInspectionHandler>(relaxed = true)
    }

    @Test
    fun `should show case view`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCase(caseId)

        with(composeTestRule) {
            setContent {
                CaseInspection(case, handler)
            }
            waitForCaseToBeShowing(caseName)
        }
    }

    @Test
    fun `should show interpretation`() = runTest {
        val text = "Go to Bondi now!"
        val case = createCase(name = "case a", id = 1L)
        case.viewableInterpretation.textGivenByRules = text

        with(composeTestRule) {
            setContent {
                CaseInspection(case, handler)
            }
            requireInterpretation(text)
        }
    }


    @Test
    fun `should handler when a rule session is started`() = runTest {
        val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
        val currentCase = createCaseWithInterpretation(
            name = "Bondi",
            id = 45L,
            diffs = diffList
        )
        with(composeTestRule) {
            setContent {
                CaseInspection(currentCase, handler)
            }

            //Given
            waitForCaseToBeShowing("Bondi")
            selectDifferencesTab()
            requireNumberOfDiffRows(1)

            //When
            clickBuildIconForRow(0)

            //Then
            verify { handler.onStartRule(diffList.get(0)) }
        }
    }
    /*

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

fun main() {
    val case = createCase(name = "Bondi", id = 45L)
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            CaseInspection(case, object : CaseInspectionHandler {
                override var caseEdited: () -> Unit = {}
                override var updateCase: (Long) -> Unit = { }
                override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                override var isCornerstone = false
                override var onInterpretationEdited: (text: String) -> Unit = { }
                override fun swapAttributes(moved: Attribute, target: Attribute) {}
            })
        }
    }
}
