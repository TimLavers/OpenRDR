package io.rippledown.interpretation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.matchers.shouldBe
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.createInterpretation
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
                InterpretationView(createInterpretation(mapOf(text to emptyList())), false)
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationView(createInterpretation(), false)
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `should highlight comment under the pointer`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val interpretation = createInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : InterpretationViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation, false, handler)
            }
            requireInterpretation(bondiComment)

            //When
            movePointerOverComment(bondiComment, textLayoutResult!!)
            waitForIdle()

            //Then
            requireCommentToBeHighlighted(bondiComment, textLayoutResult!!)
        }
    }

    @Test
    fun `should highlight comment under the pointer when showing two comments`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val malabarComment = "Malabar."
        val interpretation = createInterpretation(mapOf(bondiComment to emptyList(), malabarComment to emptyList()))
        val conclusionTexts = interpretation.conclusions().map { it.text }
        val unhighlighted = conclusionTexts.unhighlighted().text
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : InterpretationViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation, false, handler)
            }
            requireInterpretation(unhighlighted)

            //When
            movePointerOverComment(malabarComment, textLayoutResult!!)
            waitForIdle()

            //Then
            requireCommentToBeHighlighted(malabarComment, textLayoutResult!!)
        }
    }

    @Test
    fun `should not highlight a comment if the pointer is not over it`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val interpretation = createInterpretation(mapOf(bondiComment to emptyList()))
        val conclusionTexts = interpretation.conclusions().map { it.text }
        val unhighlighted = conclusionTexts.unhighlighted().text
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : InterpretationViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation, false, handler)
            }
            requireInterpretation(unhighlighted)
            movePointerOverComment(bondiComment, textLayoutResult!!)
            waitForIdle()
            requireCommentToBeHighlighted(bondiComment, textLayoutResult!!)

            //When
            movePointerToTheRightOfTheComment(bondiComment, textLayoutResult!!)
            waitForIdle()

            //Then
            requireCommentToBeNotHighlighted(bondiComment, textLayoutResult!!)
        }
    }

    @Test
    fun `should show the conditions for the conclusion under the pointer`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val malabarComment = "Great for a swim!"
        val interpretationText = "$bondiComment $malabarComment"
        val bondiConditions = listOf("Bring your flippers.", "And your sunscreeen.")
        val malabarConditions = listOf("Great for a swim!", "And a picnic.")
        val interpretation = createInterpretation(
            mapOf(
                bondiComment to bondiConditions,
                malabarComment to malabarConditions
            )
        )
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : InterpretationViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation, false, handler)
            }
            requireInterpretation(interpretationText)

            //When
            movePointerOverComment(malabarComment, textLayoutResult!!)

            //Then
            requireConditionsToBeShowing(malabarConditions)
            requireInterpretation(interpretationText)
        }
    }

    @Test
    fun `should show comment but not show any conditions for the conclusion under the pointer if there are none`() =
        runTest {
            //Given
            val bondiComment = "Best surf in the world!"
            val interpretation = createInterpretation(
                mapOf(bondiComment to listOf())
            )
            var textLayoutResult: TextLayoutResult? = null
            val handler = object : InterpretationViewHandler {
                override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                    textLayoutResult = layoutResult
                }
            }
            with(composeTestRule) {
                setContent {
                    InterpretationView(interpretation, false, handler)
                }
                requireInterpretation(bondiComment)

                //When
                movePointerOverComment(bondiComment, textLayoutResult!!)

                //Then
                requireNoConditionsToBeShowing()
                requireInterpretation(bondiComment)
            }
        }


    @Test
    fun `should show comment but not show any conditions if the pointer is not over a comment`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createInterpretation(
            mapOf(bondiComment to listOf())
        )
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : InterpretationViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation, false, handler)
            }
            requireInterpretation(bondiComment)

            //When
            movePointerToTheRightOfTheComment(bondiComment, textLayoutResult!!)

            //Then
            requireNoConditionsToBeShowing()
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should identify the comment index for a given offset`() {
        with(listOf("01234", "56789")) {
            for (i in 0..4) {
                commentIndexForOffset(i) shouldBe 0
            }
            for (i in 5..9) {
                commentIndexForOffset(i) shouldBe 1
            }
            commentIndexForOffset(10) shouldBe -1
            commentIndexForOffset(-1) shouldBe -1
        }
    }

    @Test
    fun `should highlight the first comment`() {
        with(listOf("01234", "56789")) {
            val annotatedString = highlightItem(0)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[0], BACKGROUND_COLOR)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[1], Color.Unspecified)
        }
    }

    @Test
    fun `should highlight the second comment`() {
        with(listOf("01234", "56789")) {
            val annotatedString = highlightItem(1)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[0], Color.Unspecified)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[1], BACKGROUND_COLOR)
        }
    }

}


fun main() {
    val interpretation = createCaseWithInterpretation(
        conclusionTexts = listOf("Surf's up!", "Go to Bondi now!", "Bring your flippers.")
    ).viewableInterpretation
    applicationFor {
        InterpretationView(interpretation, false)
    }
}