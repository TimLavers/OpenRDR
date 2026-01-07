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
                InterpretationActions(listOf(), setOf(), handler)
            }

            //When
            clickChangeInterpretationButton()

            //Then
            requireChangeInterpretationIconToBeShowing()
        }
    }

    @Test
    fun `should hide dropdown menu when the user selects the add comment menu then clicks cancel`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf(), setOf(), handler)
            }

            clickChangeInterpretationButton()
            clickAddCommentMenu()

            //When
            clickCancelAddNewComment()

            //Then
            requireInterpretationActionsMenuToBeNotShowing()
        }
    }

    @Test
    fun `should hide dropdown menu when the user selects the remove comment menu then clicks cancel`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf("Bondi"), setOf(), handler)
            }

            clickChangeInterpretationButton()
            clickRemoveCommentMenu()

            //When
            clickCancelRemoveComment()

            //Then
            requireInterpretationActionsMenuToBeNotShowing()
        }
    }

    @Test
    fun `should hide dropdown menu when the user selects the replace comment menu then clicks cancel`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf("Bondi"), setOf(), handler)
            }

            clickChangeInterpretationButton()
            clickReplaceCommentMenu()

            //When
            clickCancelReplaceComment()

            //Then
            requireInterpretationActionsMenuToBeNotShowing()
        }
    }

    @Test
    fun `should call handler when the user clicks on the add comment button, adds a new comment and presses OK`() {
        with(composeTestRule) {
            //Given
            setContent {
                InterpretationActions(listOf(), setOf(), handler)
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
    fun `should show all available comments when the user clicks on the add comment button if there are no comments given for the case`() {
        with(composeTestRule) {
            //Given
            val allComments = setOf("Go to Bondi", "Go to Manly", "Go to Coogee")
            setContent {
                InterpretationActions(listOf(), allComments, handler)
            }

            //When
            clickChangeInterpretationButton()
            clickAddCommentMenu()

            //Then
            requireCommentOptionsToBeDisplayed(ADD_COMMENT_PREFIX, allComments.toList())
        }
    }

    @Test
    fun `should show all available comments when the user clicks on the add comment button, apart from those given for the case`() {
        with(composeTestRule) {
            //Given
            val givenComments = listOf("Go to Manly")
            val allComments = setOf("Go to Bondi", "Go to Manly", "Go to Coogee")
            setContent {
                InterpretationActions(givenComments, allComments, handler)
            }

            //When
            clickChangeInterpretationButton()
            clickAddCommentMenu()

            //Then
            requireCommentOptionsToBeDisplayed(ADD_COMMENT_PREFIX, allComments.toList() - givenComments)
        }
    }

    @Test
    fun `should call handler when the user clicks on the replace comment button, selects an existing comment, adds a replacement comment and presses OK`() =
        runTest {
            with(composeTestRule) {
                //Given
                val bondi = "Bondi"
                val maroubra = "Maroubra"
                val coogee = "Coogee"
                val commentsForCase = listOf(bondi, maroubra)
                val allComments = setOf(bondi, maroubra, coogee)

                setContent {
                    InterpretationActions(commentsForCase, allComments, handler)
                }
                clickChangeInterpretationButton()

                //When
                clickReplaceCommentMenu()
                replaceComment(bondi, coogee)

                //Then
                verify { handler.startRuleToReplaceComment(bondi, coogee) }
            }
        }

    @Test
    fun `should call handler when the user clicks on the remove comment button, selects a comment and presses OK`() =
        runTest {
            val bondi = "Bondi"
            val maroubra = "Maroubra"
            val coogee = "Coogee"
            val commentsForCase = listOf(bondi, maroubra)
            val allComments = setOf(bondi, maroubra, coogee)
            with(composeTestRule) {
                //Given
                setContent {
                    InterpretationActions(commentsForCase, allComments, handler)
                }
                clickChangeInterpretationButton()

                //When
                clickRemoveCommentMenu()
                removeComment(maroubra)

                //Then
                verify(timeout = 1_000) { handler.startRuleToRemoveComment(maroubra) }
            }
        }
}

fun main() {
    val handler = mockk<InterpretationActionsHandler>()
    val givenComments = listOf("Bondi", "Manly", "Coogee")
    val allComments = givenComments.toSet() + setOf("Maroubra", "Bronte", "Tamarama")
    applicationFor {
        InterpretationActions(givenComments, allComments, handler)
    }
}




