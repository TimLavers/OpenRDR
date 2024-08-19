package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationActionsTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var handler: InterpretationActionsHandler

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should show dropdown menu when the user clicks on the change interpretation button`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf(), handler)
            }

            //When
            clickChangeInterpretationButton()

            //Then
            requireInterpretationActionsDropdownMenu()
        }
    }

    @Test
    fun `should call handler when the user clicks on the add comment button, adds a new comment and presses OK`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf(), handler)
            }
            clickChangeInterpretationButton()

            //When
            clickAddCommentMenu()
            addNewComment("Bondi")

            //Then
            verify(timeout = 1_000) { handler.startRuleToAddComment("Bondi") }
        }
    }

    @Test
    fun `should handler when the user clicks on the replace comment button, selects an existing comment, adds a replacement comment and presses OK`() =
        runTest {
        with(composeTestRule) {
            //Given
            val bondi = "Bondi"
            val maroubra = "Maroubra"
            val coogee = "Coogee"
            val comments = listOf(bondi, maroubra)

            setContent {
                InterpretationActions(comments, handler)
            }
            clickChangeInterpretationButton()

            //When
            clickReplaceCommentMenu()
            replaceComment(bondi, coogee)

            //Then
            verify(timeout = 1_000) { handler.startRuleToReplaceComment(bondi, coogee) }
        }
    }

    @Test
    fun `should call handler when the user clicks on the remove comment button, selects a comment and presses OK`() =
        runTest {
        val comments = listOf("Bondi", "Manly", "Coogee")
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(comments, handler)
            }
            clickChangeInterpretationButton()

            //When
            clickRemoveCommentMenu()
            removeComment("Manly")

            //Then
            verify(timeout = 1_000) { handler.startRuleToRemoveComment("Manly") }
        }
        }
}

fun main() {
    val handler = mockk<InterpretationActionsHandler>(relaxed = true)
    applicationFor {
        InterpretationActions(listOf("Bondi", "Malabar"), handler)
    }
}




