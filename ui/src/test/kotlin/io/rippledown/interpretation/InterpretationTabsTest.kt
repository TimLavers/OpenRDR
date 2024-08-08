package io.rippledown.interpretation

import InterpretationTabs
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class InterpretationTabsTest {
    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun `interpretation tab should be selected by default`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationTabs(ViewableInterpretation())
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
                InterpretationTabs(viewableInterpretation)
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `conclusions should be showing after clicking the tab`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationTabs(interpretationWithConclusions())
            }

            //When
            selectConclusionsTab()

            //Then
            requireConclusionsPanelToBeShowing()
        }
    }
}

fun main() {
    applicationFor {
        InterpretationTabs(interpretationWithConclusions())
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
