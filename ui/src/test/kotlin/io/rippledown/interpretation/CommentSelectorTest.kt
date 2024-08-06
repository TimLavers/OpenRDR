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

    private val comments = listOf("Bondi", "Malabar", "Coogee")
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
                CommentSelector("", comments, label, prefix, handler)
            }
            requireDropDownMenuToBeDisplayed()

            //Then
            requireCommentSelectorWithSelectedLabel(label)
        }
    }

    @Test
    fun `should show the selected comment`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("Bondi", comments, label, prefix, handler)
            }
            requireDropDownMenuToBeDisplayed()

            //When
            clickCommentDropDownMenu()

            //Then
            requireCommentSelectorForPrefixWithSelectedComment(prefix, "Bondi")
        }
    }

    @Test
    fun `should show all options`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", comments, label, prefix, handler)
            }
            requireCommentSelectorOptionsNotToBeDisplayed(prefix, comments)

            //When
            clickCommentDropDownMenu()

            //Then
            requireCommentSelectorOptionsToBeDisplayed(prefix, comments)
        }
    }

    @Test
    fun `selecting an option should call the handler`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", comments, label, prefix, handler)
            }
            requireCommentSelectorOptionsNotToBeDisplayed(prefix, comments)

            //When
            clickCommentDropDownMenu()
            requireCommentSelectorOptionsToBeDisplayed(prefix, comments)
            clickComment(prefix, comments[1])

            //Then
            verify { handler.onCommentSelected(comments[1]) }
            verify(exactly = 0) { handler.onCommentSelected(comments[0]) }
            verify(exactly = 1) { handler.onCommentSelected(comments[1]) }
            verify(exactly = 0) { handler.onCommentSelected(comments[2]) }
        }
    }
}


fun main() {
    val handler = mockk<CommentSelectorHandler>(relaxed = true)
    applicationFor {
        CommentSelector("Bondi", listOf("Bondi", "Malabar"), "please select a beach", "PREFIX", handler)
    }
}