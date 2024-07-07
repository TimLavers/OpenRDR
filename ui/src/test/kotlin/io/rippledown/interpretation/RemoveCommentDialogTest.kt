package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
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
    fun `should call handler to start rule when OK is pressed`() = runTest {
        val bondi = "Bondi"
        val maroubra = "Maroubra"
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(true, listOf(bondi, maroubra), handler)
                waitForIdle()
            }

            //When
            selectCommentToRemove(bondi)
            clickOKToRemoveComment()

            //Then
            verify(timeout = 1_000) { handler.startRuleToRemoveComment(bondi) }
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                RemoveCommentDialog(true, listOf(), handler)
                waitForIdle()
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
        RemoveCommentDialog(true, listOf(), mockk())
    }
}