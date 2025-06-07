@file:OptIn(ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class AnnotatedTextViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
    }

    @Test
    fun `should show non-blank interpretation`() = runTest {
        val bondiComment = "Bondi"
        with(composeTestRule) {
            setContent {
                AnnotatedTextView(
                    AnnotatedString(bondiComment),
                    INTERPRETATION_TEXT_FIELD,
                    mockk()
                )
            }
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                AnnotatedTextView(AnnotatedString(""), INTERPRETATION_TEXT_FIELD, mockk())
            }
            requireInterpretation("")
        }
    }

    @Test
    fun `should call the handler when the pointer is over a comment`() = runTest {
        //Given
        val bondiComment = "Bondi"
        val malabarComment = "Malabar"
        val commentList = listOf(bondiComment, malabarComment)
        var actualOffset = -1
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : AnnotatedTextViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }

            override fun onPointerEnter(characterOffset: Int) {
                actualOffset = characterOffset
            }

            override fun onPointerExit() {

            }
        }

        with(composeTestRule) {
            setContent {
                AnnotatedTextView(
                    text = commentList.unhighlighted(),
                    handler = handler
                )
            }
            requireInterpretation(commentList.unhighlighted().text)

            //When
            movePointerOverComment(malabarComment, textLayoutResult!!)

            //Then
            actualOffset shouldBe "$bondiComment ".length
        }
    }

    @Test
    fun `should call the handler when the pointer moves off a comment`() = runTest {
        //Given
        val bondiComment = "Bondi"
        val malabarComment = "Malabar"
        val commentList = listOf(bondiComment, malabarComment)
        var pointerExit = false
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : AnnotatedTextViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }

            override fun onPointerEnter(characterOffset: Int) {
            }

            override fun onPointerExit() {
                pointerExit = true
            }
        }

        with(composeTestRule) {
            setContent {
                AnnotatedTextView(
                    text = commentList.unhighlighted(),
                    handler = handler
                )
            }
            requireInterpretation(commentList.unhighlighted().text)
            movePointerOverComment(malabarComment, textLayoutResult!!)
            pointerExit shouldBe false

            //When
            movePointerBelowTheText(textLayoutResult!!)

            //Then
            pointerExit shouldBe true
        }
    }

    @Test
    fun `should call the handler when the pointer moves out of the text composable`() = runTest {
        //Given
        val bondiComment = "Bondi"
        val malabarComment = "Malabar"
        val commentList = listOf(bondiComment, malabarComment)
        var actualIndex = -1
        var pointerExit = false
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : AnnotatedTextViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }

            override fun onPointerEnter(characterOffset: Int) {
                actualIndex = characterOffset
            }

            override fun onPointerExit() {
                pointerExit = true
            }
        }

        with(composeTestRule) {
            setContent {
                AnnotatedTextView(
                    text = commentList.unhighlighted(),
                    handler = handler
                )
            }
            requireInterpretation(commentList.unhighlighted().text)
            movePointerOverComment(malabarComment, textLayoutResult!!)
            actualIndex shouldBe "$bondiComment ".length

            //When
            movePointerBelowTheText(textLayoutResult!!)

            //Then
            pointerExit shouldBe true
        }
    }
}