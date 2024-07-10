package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
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
    fun `should handler when the user clicks on the add comment button, adds a comment and presses OK`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf(), handler)
            }
            clickChangeInterpretationButton()

            //When
            clickAddCommentMenu()
            addNewComment("Bondi")
            clickOKToAddNewComment()

            //Then
            verify(timeout = 1_000) { handler.startRuleToAddComment("Bondi") }
        }
    }

    @Test
    fun `should handler when the user clicks on the replace comment button`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf(), handler)
            }
            clickChangeInterpretationButton()

            //When
            clickReplaceCommentMenu()

            //Then
            verify { handler.replaceComment() }
        }
    }

    @Test
    fun `should handler when the user clicks on the remove comment button`() = runTest {
        val comments = listOf("Bondi", "Manly", "Coogee")
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(comments, handler)
            }
            clickChangeInterpretationButton()

            //When
            clickRemoveCommentMenu()

            selectCommentToRemove("Manly")

            //Then
            verify { handler.startRuleToRemoveComment("Manly") }
        }
    }
}




