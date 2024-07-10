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
    private val prefix = "PREFIX"

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

            //Then
            requireCommentSelectorWithSelectedComment("Bondi")
        }
    }

    @Test
    fun `should show all options`() {
        with(composeTestRule) {
            //Given
            setContent {
                CommentSelector("", comments, label, prefix, handler)
            }

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

            //When
            clickCommentDropDownMenu()
            requireCommentSelectorOptionsToBeDisplayed(prefix, comments)
            clickComment(prefix, comments[1])

            //Then
            verify { handler.onCommentSelected(comments[1]) }
        }
    }
}


fun main() {
    val handler = mockk<CommentSelectorHandler>(relaxed = true)
    applicationFor {
        CommentSelector("Bondi", listOf("Bondi", "Malabar"), "", "PREFIX", handler)
    }
}