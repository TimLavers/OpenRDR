package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createViewableCaseWithInterpretation
import io.rippledown.utils.createViewableInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: InterpretationViewHandler

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
        every { handler.allComments() } returns setOf("Malabar.", "Bondi.")
    }

    @Test
    fun `should show change interpretation icon if the showChangeInterpretationIcon parameter is true`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    showChangeIcon = true,
                    handler
                )
            }
            requireInterpretation(bondiComment)

            //Then
            requireChangeInterpretationIconToBeShowing()
        }
    }

    @Test
    fun `should not show change interpretation icon if the showChangeInterpretationIcon parameter is false`() =
        runTest {
            //Given
            val bondiComment = "Best surf in the world!"
            val interpretation = createViewableInterpretation(
                mapOf(bondiComment to listOf())
            )
            with(composeTestRule) {
                setContent {
                    InterpretationView(
                        interpretation = interpretation,
                        showChangeIcon = false,
                        handler
                    )
                }
                requireInterpretation(bondiComment)

                //Then
                requireChangeInterpretationIconToBeNotShowing()
            }
        }

    @Test
    fun `should show change interpretation icon if not in a rule session`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    showChangeIcon = true,
                    handler
                )
            }
            requireInterpretation(bondiComment)

            //Then
            requireChangeInterpretationIconToBeShowing()
        }
    }

    @Test
    fun `should retrieve all comments`() = runTest {
        //Given
        val text = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                InterpretationView(createViewableInterpretation(mapOf(text to emptyList())), true, handler)
            }
            requireInterpretation(text)

            //Then
            verify { handler.allComments() }
        }
    }

    @Test
    fun `should show dropdown if the change interpretation icon is clicked`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation = interpretation, true, handler)
            }
            requireInterpretation(bondiComment)

            //When
            clickChangeInterpretationButton()

            //Then
            requireInterpretationActionsMenuToBeShowing()
        }
    }

    @Test
    fun `should call handler when a rule session is started to add a comment`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation = interpretation, true, handler)
            }
            requireInterpretation(bondiComment)
            clickChangeInterpretationButton()

            //When
            clickAddCommentMenu()
            val addedComment = "abc"
            addNewComment(addedComment)

            //Then
            verify { handler.startRuleToAddComment(addedComment) }
        }
    }

    @Test
    fun `should call handler when a rule session is started to remove a comment`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation = interpretation, true, handler)
            }
            requireInterpretation(bondiComment)
            clickChangeInterpretationButton()

            //When
            clickRemoveCommentMenu()
            removeComment(bondiComment)

            //Then
            verify { handler.startRuleToRemoveComment(bondiComment) }
        }
    }

    @Test
    fun `should call handler when a rule session is started to replace a comment`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(interpretation = interpretation, true, handler)
            }
            requireInterpretation(bondiComment)
            clickChangeInterpretationButton()

            //When
            clickReplaceCommentMenu()
            val replacement = "Very best surf in the world!"
            replaceComment(bondiComment, replacement)

            //Then
            verify { handler.startRuleToReplaceComment(bondiComment, replacement) }
        }
    }
}


fun main() {
    val interpretation = createViewableCaseWithInterpretation(
        conclusionTexts = listOf("Surf's up!", "Go to Bondi now!", "Bring your flippers.")
    ).viewableInterpretation
    applicationFor {
        InterpretationView(interpretation, true, mockk())
    }
}