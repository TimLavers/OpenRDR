package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.interpretation.REPLACED_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REPLACEMENT_COMMENT_PREFIX
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class ReplaceCommentDialogTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var handler: ReplaceCommentHandler

    val bondi = "Bondi"
    val maroubra = "Maroubra"
    val coogee = "Coogee"
    val bronte = "Bronte"
    val givenComments = listOf(bondi, maroubra)
    val availableComments = listOf(bondi, maroubra, coogee, bronte)

    @Before
    fun init() {
        handler = mockk()
    }

    @Test
    fun `all the given comments should be initially shown as options for the comment to be replaced`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(givenComments, availableComments, handler)
            }

            //Then
            requireCommentOptionsToBeDisplayed(REPLACED_COMMENT_PREFIX, givenComments)
        }
    }

    @Test
    fun `the options for a replacement comment should initially be all available comments except the given comments`() =
        runTest {
            with(composeTestRule) {
                //Given
                setContent {
                    ReplaceCommentDialog(givenComments, availableComments, handler)
                }

                //Then
                requireCommentOptionsToBeDisplayed(REPLACEMENT_COMMENT_PREFIX, availableComments - givenComments)
            }
        }

    @Test
    fun `the options for a replacement comment should be all available comments except the given comments and the comment being replaced`() =
        runTest {
            with(composeTestRule) {
                //Given
                setContent {
                    ReplaceCommentDialog(givenComments, availableComments, handler)
                }

                //When
                enterTextIntoTheCommentSelector(REPLACED_COMMENT_PREFIX, bondi)

                //Then
                requireCommentOptionsToBeDisplayed(
                    REPLACEMENT_COMMENT_PREFIX,
                    availableComments - givenComments - bondi
                )
            }
        }

    @Test
    fun `should call handler to start rule when OK is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(givenComments, availableComments, handler)
            }

            //When
            replaceComment(bondi, coogee)

            //Then
            verify(timeout = 1_000) { handler.startRuleToReplaceComment(bondi, coogee) }
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(listOf(), availableComments, handler)
            }

            //When
            clickCancelReplaceComment()

            //Then
            verify { handler.cancel() }
        }
    }

    @Test
    fun `should disable the OK button until a comment to be replaced is selected`() {
        val givenComments = listOf("Bondi", "Maroubra")
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(givenComments, listOf(), handler)
            }

            //When
            requireCommentOptionsToBeDisplayed(REPLACED_COMMENT_PREFIX, givenComments)
            enterTextIntoTheCommentSelector(REPLACED_COMMENT_PREFIX, "Coogee")

            //Then
            requireOKButtonOnReplaceCommentDialogToBeDisabled()
        }
    }

    @Test
    fun `should disable the OK button if no comment to be replaced is selected`() {
        val givenComments = listOf("Bondi", "Maroubra")
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(givenComments, listOf(), handler)
            }

            //When
            requireCommentOptionsToBeDisplayed(REPLACED_COMMENT_PREFIX, givenComments)

            //Then
            requireOKButtonOnReplaceCommentDialogToBeDisabled()
        }
    }


}

fun main() {
    val bondi = "Bondi"
    val maroubra = "Maroubra"
    val handler = object : ReplaceCommentHandler {
        override fun startRuleToReplaceComment(toBeReplaced: String, replacement: String) {
            println("startRuleToReplaceComment")
        }

        override fun cancel() {
            println("cancel")
        }
    }
    applicationFor {
        ReplaceCommentDialog(listOf(), listOf(bondi, maroubra), handler)
    }
}