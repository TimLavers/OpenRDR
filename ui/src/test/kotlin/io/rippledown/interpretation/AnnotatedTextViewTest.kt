@file:OptIn(ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.utils.waitUntilAsserted
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
            y

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
            movePointerBelowTheText(textLayoutResult)

            //Then
            pointerExit shouldBe true
        }
    }

    @Test
    fun `should use updated handler when handler changes`() = runTest {
        //Given
        val bondiComment = "Bondi"
        val malabarComment = "Malabar"
        val commentList = listOf(bondiComment, malabarComment)
        var offsetFromFirstHandler = -1
        var offsetFromSecondHandler = -1
        var textLayoutResult: TextLayoutResult? = null

        val firstHandler = object : AnnotatedTextViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }

            override fun onPointerEnter(characterOffset: Int) {
                offsetFromFirstHandler = characterOffset
            }

            override fun onPointerExit() {}
        }
        val secondHandler = object : AnnotatedTextViewHandler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }

            override fun onPointerEnter(characterOffset: Int) {
                offsetFromSecondHandler = characterOffset
            }

            override fun onPointerExit() {}
        }
        val handlerState: MutableState<AnnotatedTextViewHandler> = mutableStateOf(firstHandler)

        with(composeTestRule) {
            setContent {
                AnnotatedTextView(
                    text = commentList.unhighlighted(),
                    handler = handlerState.value
                )
            }
            requireInterpretation(commentList.unhighlighted().text)

            // Hover over first comment - should call first handler
            movePointerOverComment(bondiComment, textLayoutResult!!)
            waitUntilAsserted { offsetFromFirstHandler shouldBe 0 }
            offsetFromSecondHandler shouldBe -1

            // Swap to the second handler
            runOnIdle { handlerState.value = secondHandler }
            waitForIdle()

            // Move pointer to second comment - should call second handler, not first
            movePointerOverComment(malabarComment, textLayoutResult)
            waitUntilAsserted { offsetFromSecondHandler shouldBe "$bondiComment ".length }
        }
    }
}