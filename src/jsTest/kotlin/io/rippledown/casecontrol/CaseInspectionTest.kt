package io.rippledown.casecontrol

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.cornerstoneview.requireCornerstoneCaseToBeShowing
import io.rippledown.interpretation.*
import io.rippledown.model.CaseId
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
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
    fun shouldUpdateTheCaseWhenTheRuleIsFinished() = runTest {
        val diffList = DiffList(listOf(Addition("Go to Bondi now!")))
        val currentCase = createCaseWithInterpretation(
            name = "Bondi",
            id = 45L,
            diffs = diffList
        )
        val config = config {

        }
        var idOfCaseToBeUpdated = -1L

        val fc = FC {
            CaseInspection {
                case = currentCase
                ruleSessionInProgress = { _ -> }
                updateCase = { id ->
                    idOfCaseToBeUpdated = id
                }
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
            clickBuildIconForRow(0)
            requireDoneButtonShowing()

            //When
            clickDoneButton()

            //Then
            idOfCaseToBeUpdated shouldBe currentCase.id
        }
    }


}