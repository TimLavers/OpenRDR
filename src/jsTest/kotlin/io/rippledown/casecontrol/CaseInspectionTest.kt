package io.rippledown.casecontrol

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseNotToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
import io.rippledown.interpretation.*
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CaseInspectionTest {

    @Test
    fun shouldShowCaseView() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val currentCase = createCase(caseId)

        val fc = FC {
            CaseInspection {
                case = currentCase
            }
        }

        checkContainer(fc) { container ->
            with(container) {
                requireCaseToBeShowing(caseName)
            }
        }
    }

    @Test
    fun shouldShowInterpretation() = runTest {
        val text = "Go to Bondi now!"
        val rdrCase = createCase(name = "case a", id = 1L)
        rdrCase.viewableInterpretation.textGivenByRules = text
        val fc = FC {
            CaseInspection {
                case = rdrCase
            }
        }
        createRootFor(fc).requireInterpretation(text)
    }


    @Test
    fun shouldCallRuleSessionInProgressWhenRuleIsStarted() = runTest {
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
                scope = this@runTest
                ruleSessionInProgress = { started ->
                    inProgress = started
                }
            }
        }
        with(createRootFor(fc)) {
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

    @Test
    fun shouldShowConditionSelectorWhenRuleIsStarted() = runTest {
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
                scope = this@runTest
            }
        }
        with(createRootFor(fc)) {
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

    @Test
    fun shouldCallConditionHintsApiWhenRuleIsStarted() = runTest {
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
                scope = this@runTest
            }
        }
        with(createRootFor(fc)) {
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

    @Test
    fun shouldCallStartRuleSessionApiWhenRuleIsStarted(): TestResult {
        val caseId = 45L
        return runTest {
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
                    scope = this@runTest
                }
            }
            with(createRootFor(fc)) {
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
    fun shouldShowCornerstoneWhenRuleSessionIsStarted() = runTest {
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
                scope = this@runTest
                ruleSessionInProgress = { _ -> }
            }
        }
        with(createRootFor(fc)) {
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

    @Test
    fun shouldNotShowCornerstoneWhenRuleSessionIsFinished() = runTest {
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
                scope = this@runTest
                ruleSessionInProgress = { _ -> }
                updateCase = { _ -> }
            }
        }
        with(createRootFor(fc)) {
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

    @Test
    fun shouldCallBuildRuleWhenDoneButtonIsClickedOnConditionSelector() = runTest {
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
                scope = this@runTest
            }
        }
        with(createRootFor(fc)) {
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


}