package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.utils.applicationFor
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class AddCommentDialogTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var handler: AddCommentHandler

    @Before
    fun init() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should call handler to start rule when OK is pressed`() {
        with(composeTestRule) {
            //Given
            setContent {
                AddCommentDialog(true, handler)
            }

            //When
            addNewComment("Bondi")
            clickOKToAddNewComment()

            //Then
            verify { handler.startRuleToAddComment("Bondi") }
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() {
        with(composeTestRule) {
            //Given
            setContent {
                AddCommentDialog(true, handler)
            }

            //When
            clickCancelAddNewComment()

            //Then
            verify { handler.cancel() }
        }
    }
}

fun main() {
    applicationFor {
        AddCommentDialog(true, mockk())
    }
}