package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
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
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should call handler to start rule when OK is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                AddCommentDialog(true, handler)
                waitForIdle()
            }

            //When
            addNewComment("Bondi")
            onNodeWithText("OK").performClick()

            //Then
            verify(timeout = 2_000) { handler.startRuleToAddComment("Bondi") }
        }
    }

    /*
        @Test
        fun `should call handler to cancel when Cancel is pressed`()= runTest {
            with(composeTestRule) {
                //Given
                setContent {
                    AddCommentDialog(true, handler)
                    waitForIdle()
                }

                //When
                clickCancelAddNewComment()

                //Then
                verify { handler.cancel() }
            }
        }
    */
}

fun main() {
    applicationFor {
        AddCommentDialog(true, mockk())
    }
}