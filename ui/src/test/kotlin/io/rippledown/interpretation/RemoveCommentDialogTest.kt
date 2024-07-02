package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.utils.applicationFor
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
    fun `should call handler to start rule when OK is pressed`() {
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(true, handler)
            }

            //When
            selectCommentToRemove("Bondi")
            clickOKToRemoveComment()

            //Then
            verify { handler.startRuleToRemoveComment("Bondi") }
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() {
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(true, handler)
            }

            //When
            clickCancelRemoveComment()

            //Then
            verify { handler.cancel() }
        }
    }
}

fun main() {
    applicationFor {
        RemoveCommentDialog(true, mockk())
    }
}