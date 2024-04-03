package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Conclusion
import io.rippledown.model.interpretationview.ViewableInterpretation
import org.junit.Rule
import kotlin.test.Test

class ConclusionsViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should show the comments and conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                ConclusionsView(interpretation())
            }

            //Then
            requireComment(0, "This is conclusion 11")
            requireConditionForComment(0, 0, "Condition 111")
            requireConditionForComment(0, 1, "Condition 112")

            requireComment(1, "This is conclusion 12")
            requireConditionForComment(1, 0, "Condition 121")
            requireConditionForComment(1, 1, "Condition 122")

            requireComment(2, "This is conclusion 21")
            requireConditionForComment(2, 0, "Condition 211")
            requireConditionForComment(2, 1, "Condition 212")

            requireComment(3, "This is conclusion 22")
            requireConditionForComment(3, 0, "Condition 221")
            requireConditionForComment(3, 1, "Condition 222")
        }
    }

    @Test
    fun `should be able to collapse the comments`() {
        with(composeTestRule) {
            //Given
            setContent {
                ConclusionsView(interpretation())
            }
            //
            //When
            clickComment(0, "This is conclusion 11")
            clickComment(1, "This is conclusion 12")
            clickComment(2, "This is conclusion 21")
            clickComment(3, "This is conclusion 22")

            //Then
            requireConditionNotShowing(0, 0, "Condition 111")
            requireConditionNotShowing(0, 1, "Condition 112")
            requireConditionNotShowing(1, 0, "Condition 121")
            requireConditionNotShowing(1, 1, "Condition 122")
            requireConditionNotShowing(2, 0, "Condition 211")
            requireConditionNotShowing(2, 1, "Condition 212")
            requireConditionNotShowing(3, 0, "Condition 221")
            requireConditionNotShowing(3, 1, "Condition 222")
        }
    }
}

private fun interpretation(): ViewableInterpretation {
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
    }
    return interpretation
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ConclusionsView(interpretation())
        }
    }
}