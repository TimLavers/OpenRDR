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
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should show the specified label`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(false, "", options, label, prefix, handler)
            }
            requireDropDownMenuToBeDisplayed()

            //Then
            requireCommentSelectorLabel(label)
        }
    }

    @Test
    fun `should show all the options if the text field is blank and then is clicked`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(false, "", options, label, prefix, handler)
            }

            //When
            clickCommentDropDownMenu()

            //Then
            requireCommentOptionsToBeDisplayed(prefix, options)
        }
    }

    @Test
    fun `selecting an option should call the handler`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(false, "", options, label, prefix, handler)
            }
            requireCommentOptionsNotToExist(prefix, options)

            //When
            clickCommentDropDownMenu()
            requireCommentOptionsToBeDisplayed(prefix, options)
            clickComment(prefix, options[1])

            //Then
            verify { handler.onCommentChanged(options[1]) }
            verify(exactly = 0) { handler.onCommentChanged(options[0]) }
            verify(exactly = 1) { handler.onCommentChanged(options[1]) }
            verify(exactly = 0) { handler.onCommentChanged(options[2]) }
        }
    }

    @Test
    fun `options should be filtered by the current text`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(false, "Mal", options, label, prefix, handler)
            }

            //When
            clickCommentDropDownMenu()

            //Then
            requireCommentOptionsToBeDisplayed(prefix, listOf("Malabar"))
        }
    }

    @Test
    fun `entering text should call the handler`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(true, "", options, label, prefix, handler)
            }

            //When
            val newComment = "Go to Bronte"
            enterTextIntoTheCommentSelector(newComment)

            //Then
            verify { handler.onCommentChanged(newComment) }
        }
    }

    @Test
    fun `entering text with the options showing should filter the options`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(true, "", options, label, prefix, handler)
            }

            //When
            clickCommentDropDownMenu()
            requireCommentOptionsToBeDisplayed(prefix, options)
            enterTextIntoTheCommentSelector("mal")

            //Then
            requireCommentOptionsToBeDisplayed(prefix, listOf("Malabar"))
        }
    }

    @Test
    fun `should be able to scroll the list of options`() = runTest {
        val options = (1..100).map { "Option $it" }
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector(false, "", options, label, prefix, handler)
            }

            //When
            clickCommentDropDownMenu()
            requireCommentOptionsNotToBeDisplayed(prefix, options.subList(95, 99))
            scrollToOption(prefix, options[99])

            //Then
            requireCommentOptionsToBeDisplayed(prefix, options.subList(95, 99))
        }
    }
}


fun main() {
    val handler = mockk<CommentSelectorHandler>(relaxed = true)
    val options = (1..100).map {
        "Option $it"
    }
    applicationFor {
        CommentSelector(true, "", options, "please select a beach", "PREFIX", handler)
    }
}