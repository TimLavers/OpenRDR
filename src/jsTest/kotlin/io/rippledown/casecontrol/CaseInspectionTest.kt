package io.rippledown.casecontrol

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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import main.Api
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseInspectionTest {
    @Test
    fun shouldShowCaseView(): TestResult {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val currentCase = createCase(caseId)

        val fc = FC {
            CaseInspection {
                case = currentCase
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                requireCaseToBeShowing(caseName)
            }
        }
    }

    @Test
    fun shouldShowInterpretation(): TestResult {
        val text = "Go to Bondi now!"
        val rdrCase = createCase(name = "case a", id = 1L)
        rdrCase.viewableInterpretation.textGivenByRules = text
        val fc = FC {
            CaseInspection {
                case = rdrCase
            }
        }
        return runReactTest(fc) { container ->
            container.requireInterpretation(text)
        }
    }


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
    }
}