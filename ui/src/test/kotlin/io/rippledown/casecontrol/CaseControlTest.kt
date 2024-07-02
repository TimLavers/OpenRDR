package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.interpretation.*
import io.rippledown.main.DEFAULT_WINDOW_SIZE
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Replacement
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.CornerstoneStatus
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
    fun `should show the interpretative report of the case`() = runTest {
        val name = "case A"
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(name, 1, listOf(bondiComment))

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    conditionHints = listOf(),
                    handler = handler
                )
            }

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show the comments of the case`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val coogeeComment = "Go to Coogee"
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment, coogeeComment))
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            //When
            selectConclusionsTab()

            //Then
            requireComment(0, bondiComment)
            requireComment(1, coogeeComment)
        }
    }

    @Test
    fun `should save the case when its interpretation changes`() = runTest {
        val caseA = "case A"
        val caseId = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId)
        val bondiComment = "Go to Bondi"
        val manlyComment = "Go to Manly"
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        val slot = slot<ViewableCase>()
        every { handler.saveCase(capture(slot)) } answers { Unit }
        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = case,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            //Given
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
//        coEvery { handler.getCase(1) } returns originalCase

        val slot = slot<ViewableCase>()
        coEvery { handler.saveCase(capture(slot)) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = null,
                    conditionHints = listOf(),
                    handler = handler
                )
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
        val caseWithDiff = ViewableCase(
            case = originalCase.case,
            viewableInterpretation = ViewableInterpretation(originalCase.case.interpretation).apply {
                verifiedText = manlyComment
                diffList = DiffList(listOf(Replacement(bondiComment, manlyComment)))
            },
            viewProperties = originalCase.viewProperties
        )

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = caseWithDiff,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            requireInterpretation(manlyComment)

            //Then
            requireBadgeOnDifferencesTabToShow(1)
        }
    }

    @Test
    fun `should show case view`() = runTest {
        val viewableCase = createCaseWithInterpretation(
            name = "case 1",
            id = 1,
            conclusionTexts = listOf()
        )
        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    conditionHints = listOf(),
                    handler = handler
                )
            }
            waitForCaseToBeShowing("case 1")
        }
    }


}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(size = DEFAULT_WINDOW_SIZE)
        ) {
            val handler = mockk<CaseControlHandler>(relaxed = true)

            val caseName = "Bondi"
            val id = 45L
            val beachComment = "Enjoy the beach."
            val bondiComment = "Go to Bondi now!"
            val diffList = DiffList(
                listOf(
                    Unchanged(beachComment),
                    Addition(bondiComment),
                )
            )
            val viewableCase = createCaseWithInterpretation(
                name = caseName,
                id = id,
                conclusionTexts = listOf(bondiComment),
                diffs = diffList
            )
            coEvery { handler.selectCornerstone(any()) } returns viewableCase
            val condition = hasCurrentValue(1, Attribute(2, "Surf 1"))
            CaseControl(
                currentCase = viewableCase,
                conditionHints = listOf(condition),
                cornerstoneStatus = CornerstoneStatus(viewableCase, 42, 84),
                handler = handler
            )
        }
    }
}