package io.rippledown.interpretation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Conclusion
import io.rippledown.model.interpretationview.ViewableInterpretation

class TreeRendererTest {

}

@OptIn(ExperimentalAnimationApi::class)
fun main() {
    val interpretation = mockk<ViewableInterpretation>()
    val c11 = Conclusion(11, "This is conclusion 11")
    val c12 = Conclusion(12, "This is conclusion 12")
    val c21 = Conclusion(21, "This is conclusion 21")
    val c22 = Conclusion(22, "This is conclusion 22")
    every { interpretation.conclusions() } returns setOf(c11, c12, c21, c22)
    every { interpretation.conditionsForConclusion(c11) } returns listOf("Condition 111", "Condition 112")
    every { interpretation.conditionsForConclusion(c12) } returns listOf("Condition 121", "Condition 122")
    every { interpretation.conditionsForConclusion(c21) } returns listOf("Condition 211", "Condition 212")
    every { interpretation.conditionsForConclusion(c22) } returns listOf("Condition 221", "Condition 222")

    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ConclusionsView(interpretation)
        }
    }
}