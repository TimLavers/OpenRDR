package io.rippledown.interpretation

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.diffview.clickBuildIconForRow
import io.rippledown.diffview.moveMouseOverRow
import io.rippledown.diffview.requireNumberOfDiffRows
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationTabsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: InterpretationTabsHandler

    @Before
    fun setUp() {
        handler = mockk<InterpretationTabsHandler>(relaxed = true)
    }

    @Test
    fun `interpretation tab should be selected by default`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationTabs(ViewableInterpretation(), handler)
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `the latest text of the interpretation should be showing by default`() = runTest {
        val text = "Go to Bondi now!"
        val viewableInterpretation = ViewableInterpretation(Interpretation()).apply { verifiedText = text }
        with(composeTestRule) {
            setContent {
                InterpretationTabs(viewableInterpretation, handler)
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `the handler should be called if the interpretation text is edited`() = runTest {
        val viewableInterpretation = ViewableInterpretation(Interpretation())
        with(composeTestRule) {
            setContent {
                InterpretationTabs(viewableInterpretation, handler)
            }
            //Given
            requireInterpretation("")

            //When
            val newText = "...and bring your flippers!"
            enterInterpretation(newText)

            //Then
            verify { handler.onInterpretationEdited(newText) }
        }
    }

    @Test
    fun `should not call the handler if no changes have been made to the interpretation`() = runTest {
        //Given
        val originalInterpretation = ViewableInterpretation(Interpretation())

        with(composeTestRule) {
            setContent {
                InterpretationTabs(originalInterpretation, handler)
            }
            requireInterpretation("")

            //When

            //Then
            verify { handler.onInterpretationEdited wasNot Called }
        }
    }

    @Test
    fun `conclusions should be showing after clicking the tab`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithConclusions(), handler)
            }

            //When
            selectConclusionsTab()

            //Then
            requireConclusionsPanelToBeShowing()
        }
    }

    @Test
    fun `badge on differences icon should not show if there are no differences`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithDifferences(0), handler)
            }

            //Then
            requireBadgeOnDifferencesTabNotToBeShowing()
        }
    }

    @Test
    fun `should not show difference tab if a cornerstone case`() = runTest {
        with(composeTestRule) {
            every { handler.isCornerstone } returns true

            //Given
            setContent {
                InterpretationTabs(interpretationWithDifferences(0), handler)
            }

            //Then
            requireNoDifferencesTab()
        }
    }

    @Test
    fun `badge on differences icon should show the expected number of differences`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithDifferences(42), handler)
            }

            //Then
            requireBadgeOnDifferencesTabToShow(42)
        }
    }

    @Test
    fun `should be able to select the differences tab`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithDifferences(42), handler)
            }
            requireDifferencesTabToBeNotShowing()

            //When
            selectDifferencesTab()

            //Then
            requireDifferencesTabToBeShowing()
        }
    }

    @Test
    fun `difference view should not show any changes if the DiffList is empty`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithDifferences(0), handler)
            }
            requireDifferencesTabToBeNotShowing()

            //When
            selectDifferencesTab()
            requireDifferencesTabToBeShowing()

            //Then
            requireNumberOfDiffRows(0)
        }
    }

    @Test
    fun `difference view should show a row for each diff in the DiffList`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                //Using LazyColumn, so keep the visible rows to a minimum
                InterpretationTabs(interpretationWithDifferences(10), handler)
            }
            requireDifferencesTabToBeNotShowing()

            //When
            selectDifferencesTab()
            requireDifferencesTabToBeShowing()

            //Then
            requireNumberOfDiffRows(10)
        }
    }

    @Test
    fun `difference view should update when the interpretation is edited`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                //Using LazyColumn, so keep the visible rows to a minimum
                InterpretationTabs(interpretationWithDifferences(10), handler)
            }
            requireDifferencesTabToBeNotShowing()

            //When
            selectDifferencesTab()
            requireDifferencesTabToBeShowing()

            //Then
            requireNumberOfDiffRows(10)
        }
    }

    @Test
    fun `handler should be called when the build icon is clicked on the Difference View`() = runTest {
        val unchangedText = "Go to Bondi now!"
        val addedText = "Bring your flippers!"
        val removedText = "Sun is shining."
        val replacedText = "Surf's up!"
        val replacementText = "Surf's really up!"
        val differenceList = DiffList(
            listOf(
                Unchanged(unchangedText),
                Addition(addedText),
                Removal(removedText),
                Replacement(replacedText, replacementText)
            )
        )
        val interpretationWithDiffs =
            ViewableInterpretation().apply { diffList = differenceList }

        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithDiffs, handler)
            }
            requireDifferencesTabToBeNotShowing()
            selectDifferencesTab()
            requireDifferencesTabToBeShowing()
            requireNumberOfDiffRows(4)

            //When
            moveMouseOverRow(2)
            clickBuildIconForRow(2)

            //Then
            verify { handler.onStartRule(differenceList[2]) }
        }
    }

    /*

@Test
    fun conclusionsTabCanBeSelected(): TestResult {
        val text = "Go to Bondi."
        val interp = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(1, text)))
        }
        val fc = FC {
            InterpretationTabs {
                interpretation = ViewableInterpretation(interp)
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                selectConclusionsTab()
                requireTreeItems(text)
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
            InterpretationTabs(interpretationWithConclusions(), object : InterpretationTabsHandler {
                override fun onStartRule(selectedDiff: Diff) {}
                override var isCornerstone = false
                override var onInterpretationEdited: (text: String) -> Unit = { }

            })
        }
    }
}

private fun interpretationWithConclusions(): ViewableInterpretation {
    val interpretation = mockk<ViewableInterpretation>()
    val c11 = Conclusion(11, "This is conclusion 11")
    val c12 = Conclusion(12, "This is conclusion 12")
    val c21 = Conclusion(21, "This is conclusion 21")
    val c22 = Conclusion(22, "This is conclusion 22")
    with(interpretation) {
        every { conclusions() } returns setOf(c11, c12, c21, c22)
        every { conditionsForConclusion(c11) } returns listOf("Condition 111", "Condition 112")
        every { conditionsForConclusion(c12) } returns listOf("Condition 121", "Condition 122")
        every { conditionsForConclusion(c21) } returns listOf("Condition 211", "Condition 212")
        every { conditionsForConclusion(c22) } returns listOf("Condition 221", "Condition 222")
        every { latestText() } returns "Go to Bondi"
        every { numberOfChanges() } returns 0
    }
    return interpretation
}

private fun interpretationWithDifferences(numberOfDiffs: Int): ViewableInterpretation {
    val interpretation = mockk<ViewableInterpretation>()
    with(interpretation) {
        every { latestText() } returns "Go to Malabar"
        every { numberOfChanges() } returns numberOfDiffs
        every { diffList } returns DiffList((0..numberOfDiffs - 1).map {
            Addition("Addition $it")
        }
        )
    }
    return interpretation
}
