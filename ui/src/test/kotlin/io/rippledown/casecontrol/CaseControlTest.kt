package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.interpretation.requireComment
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.interpretation.selectConclusionsTab
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
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
    applicationFor {
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