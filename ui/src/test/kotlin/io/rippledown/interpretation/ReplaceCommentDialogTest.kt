package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
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
    val availableComments = listOf(bondi, maroubra)

    @Before
    fun init() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should call handler to start rule when OK is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(availableComments, handler)
            }

            //When
            clickCommentDropDownMenu()
            replaceComment(bondi, maroubra)
            clickOKToReplaceComment()

            //Then
            verify { handler.startRuleToReplaceComment(bondi, maroubra) }
        }
    }

    @Test
    fun `should call handler to cancel when Cancel is pressed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                ReplaceCommentDialog(availableComments, handler)
            }

            //When
            clickCancelReplaceComment()

            //Then
            verify { handler.cancel() }
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
        ReplaceCommentDialog(listOf(bondi, maroubra), handler)
    }
}