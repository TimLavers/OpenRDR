package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.interpretation.ADD_COMMENT_PREFIX
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class AddCommentDialogTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var handler: AddCommentHandler

    @Before
    fun init() {
        handler = mockk()
    }

    @Test
    fun `should call handler to start rule when OK is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                AddCommentDialog(listOf(), handler)
            }

            //When
            addNewComment("Bondi")

            //Then
            verify { handler.startRuleToAddComment("Bondi") }
        }
    }

    @Test
    fun `should initially display all available comments`() = runTest {
        with(composeTestRule) {
            //Given
            val availableComments = listOf("Bondi", "Manly", "Coogee")
            setContent {
                AddCommentDialog(availableComments, handler)
            }

            //Then
            requireCommentOptionsToBeDisplayed(ADD_COMMENT_PREFIX, availableComments)
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                AddCommentDialog(listOf(), handler)
            }

            //When
            clickCancelAddNewComment()

            //Then
            verify { handler.cancel() }
        }
    }
}

fun main() {
    val availableComments = listOf("Bondi", "Manly", "Coogee")
    applicationFor {
        AddCommentDialog(availableComments, mockk())
    }
}