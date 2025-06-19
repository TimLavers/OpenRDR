package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class CommentSelectorTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var handler: CommentSelectorHandler

    private val options = listOf("Bondi", "Malabar", "Coogee")
    private val label = "Select a comment"
    private val prefix = "PREFIX_"

    @Before
    fun setUp() {
        handler = mockk()
    }

//    @Test
    fun `should show the specified label`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", options, label, prefix, handler = handler)
            }

            //Then
            requireCommentSelectorLabel(label)
        }
    }

//    @Test
    fun `should show all the options if the text field is blank`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", options, label, prefix, handler = handler)
            }

            //Then
            requireCommentOptionsToBeDisplayed(prefix, options)
        }
    }

//    @Test
    fun `selecting an option should call the handler`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", options, label, prefix, handler = handler)
            }

            //When
            requireCommentOptionsToBeDisplayed(prefix, options)
            clickComment(prefix, options[1])

            //Then
            verify { handler.onCommentChanged(options[1]) }
            verify(exactly = 0) { handler.onCommentChanged(options[0]) }
            verify(exactly = 1) { handler.onCommentChanged(options[1]) }
            verify(exactly = 0) { handler.onCommentChanged(options[2]) }
        }
    }

//    @Test
    fun `options should be filtered by the current text`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("Mal", options, label, prefix, handler = handler)
            }

            //Then
            requireCommentOptionsToBeDisplayed(prefix, listOf("Malabar"))
        }
    }

//    @Test
    fun `entering text should call the handler`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", options, label, prefix, handler = handler)
            }

            //When
            val newComment = "Go to Bronte"
            enterTextIntoTheCommentSelector(prefix, newComment)

            //Then
            verify { handler.onCommentChanged(newComment) }
        }
    }

//    @Test
    fun `should be able to scroll the list of options`() = runTest {
        val options = (1..100).map { "Option $it" }
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", options, label, prefix, handler = handler)
            }

            //When
            requireCommentOptionsNotToBeDisplayed(prefix, options.subList(90, 95))

            scrollToOption(prefix, options[95])

            //Then
            requireCommentOptionsToBeDisplayed(prefix, options.subList(90, 95))
        }
    }
}


fun main() {
    val handler = mockk<CommentSelectorHandler>()
    val options = (1..100).map {
        "Option $it"
    }
    applicationFor {
        CommentSelector("", options, "please select a beach", "PREFIX", handler = handler)
    }
}