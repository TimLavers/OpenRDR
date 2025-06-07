package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.decoration.BACKGROUND_COLOR
import io.rippledown.utils.createInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@ExperimentalFoundationApi
class ReadonlyInterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: ReadonlyInterpretationViewHandler
    lateinit var modifier: Modifier

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
        modifier = Modifier.fillMaxWidth()

    }

    @Test
    fun `should show non-blank interpretation`() = runTest {
        val text = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createInterpretation(mapOf(text to emptyList())),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(text)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(createInterpretation(), modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone("")
        }
    }

    @Test
    fun `should highlight comment under the pointer`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val interpretation = createInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }

        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(bondiComment)

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
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(unhighlighted)

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
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(unhighlighted)
            movePointerOverComment(bondiComment, textLayoutResult!!)
            waitForIdle()
            requireCommentToBeHighlighted(bondiComment, textLayoutResult!!)

            //When
            movePointerToTheRightOfTheComment(bondiComment, textLayoutResult!!)
            waitForIdle()

            //Then
            requireCommentToBeNotHighlighted(textLayoutResult!!)
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
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(interpretationText)

            //When
            movePointerOverComment(malabarComment, textLayoutResult!!)

            //Then
            requireConditionsToBeShowing(malabarConditions)
            requireInterpretationForCornerstone(interpretationText)
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
            val handler = object : ReadonlyInterpretationViewHandler by handler {
                override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                    textLayoutResult = layoutResult
                }
            }
            with(composeTestRule) {
                setContent {
                    ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
                }
                requireInterpretationForCornerstone(bondiComment)

                //When
                movePointerOverComment(bondiComment, textLayoutResult!!)

                //Then
                requireNoConditionsToBeShowing()
                requireInterpretationForCornerstone(bondiComment)
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
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(bondiComment)

            //When
            movePointerToTheRightOfTheComment(bondiComment, textLayoutResult!!)

            //Then
            requireNoConditionsToBeShowing()
            requireInterpretationForCornerstone(bondiComment)
        }
    }

    @Test
    fun `should not show change interpretation icon`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            //When
            setContent {
                ReadonlyInterpretationView(interpretation = interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(bondiComment)

            //Then
            requireChangeInterpretationIconToBeNotShowing()
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