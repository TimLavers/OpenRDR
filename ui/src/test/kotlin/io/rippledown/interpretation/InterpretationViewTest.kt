package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import io.rippledown.model.Conclusion
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
    }

    @Test
    fun `should show non-blank interpretation`() = runTest {
        val text = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                InterpretationView(listOf(Conclusion(0, text)))
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationView(listOf())
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `should highlight the conclusion under the pointer`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val malabarComment = "Great for a swim!"
        val conclusions = listOf(Conclusion(0, bondiComment), Conclusion(1, malabarComment))
        val conclusionTexts = conclusions.map { it.text }
        val unhighlighted = conclusionTexts.unhighlighted().text
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : InterpretationViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                InterpretationView(conclusions, handler)
            }
            requireInterpretation(unhighlighted)

            //When
            movePointerOverComment(malabarComment, textLayoutResult!!)

            //Then
            requireCommentToBeNotHighlighted(bondiComment, textLayoutResult!!)
            requireCommentToBeHighlighted(malabarComment, textLayoutResult!!)
        }
    }

}

fun main() {
    applicationFor {
        InterpretationView(
            listOf(
                Conclusion(0, "Surf's up!"),
                Conclusion(1, "Go to Bondi now!"),
                Conclusion(2, "Bring your flippers.")
            ),
        )
    }
}