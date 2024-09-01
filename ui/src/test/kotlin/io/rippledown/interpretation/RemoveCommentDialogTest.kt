package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.interpretation.REMOVE_COMMENT_PREFIX
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class RemoveCommentDialogTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var handler: RemoveCommentHandler

    @Before
    fun init() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should show the options`() {
        val bondi = "Bondi"
        val maroubra = "Maroubra"
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(listOf(bondi, maroubra), handler)
            }

            //Then
            requireCommentOptionsToBeDisplayed(REMOVE_COMMENT_PREFIX, listOf(bondi, maroubra))
        }
    }

    @Test
    fun `should call handler to start rule when OK is pressed`() {
        val bondi = "Bondi"
        val maroubra = "Maroubra"
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(listOf(bondi, maroubra), handler)
            }

            //When
            requireCommentOptionsToBeDisplayed(REMOVE_COMMENT_PREFIX, listOf(bondi, maroubra))
            removeComment(bondi)

            //Then
            verify { handler.startRuleToRemoveComment(bondi) }
        }
    }

    @Test
    fun `should disable the OK button until a comment is selected`() {
        val bondi = "Bondi"
        val maroubra = "Maroubra"
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(listOf(bondi, maroubra), handler)
            }

            //When
            requireCommentOptionsToBeDisplayed(REMOVE_COMMENT_PREFIX, listOf(bondi, maroubra))
            enterTextIntoTheCommentSelector(REMOVE_COMMENT_PREFIX, "Coogee")

            //Then
            requireOKButtonOnRemoveCommentDialogToBeDisabled()
        }
    }

    @Test
    fun `should disable the OK button if no comment is selected`() {
        val bondi = "Bondi"
        val maroubra = "Maroubra"
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(listOf(bondi, maroubra), handler)
            }

            //When
            requireCommentOptionsToBeDisplayed(REMOVE_COMMENT_PREFIX, listOf(bondi, maroubra))

            //Then
            requireOKButtonOnRemoveCommentDialogToBeDisabled()
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(listOf(), handler)
            }

            //When
            clickCancelRemoveComment()

            //Then
            verify { handler.cancel() }
        }
    }
}

fun main() {
    val bondi = "Bondi"
    val maroubra = "Maroubra"
    val handler = object : RemoveCommentHandler {
        override fun startRuleToRemoveComment(comment: String) {
            println("startRuleToRemoveComment: $comment")
        }

        override fun cancel() {
            println("cancel")
        }
    }
    applicationFor {
        RemoveCommentDialog(listOf(bondi, maroubra), handler)
    }
}