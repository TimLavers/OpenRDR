package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.interpretation.COMMENT_PENDING_ADD_PREFIX
import io.rippledown.constants.interpretation.COMMENT_PENDING_REMOVE_PREFIX
import io.rippledown.constants.interpretation.COMMENT_PENDING_REPLACE_PREFIX
import io.rippledown.constants.interpretation.UNRESOLVED_VARIABLE_TOOLTIP
import io.rippledown.model.IntRangeData
import io.rippledown.model.RenderedComment
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.utils.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * The comments are shown as a two column table of the name of the comment
 * attribute that gave each comment and the comment itself, so these tests read
 * and hover its rows. The rows themselves are computed by
 * [commentRowsToDisplay], which [CommentRowsTest] covers.
 */
@ExperimentalFoundationApi
class ReadonlyInterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: ReadonlyInterpretationViewHandler
    lateinit var modifier: Modifier

    private val firstName = commentAttributeName(FIRST_COMMENT_ATTRIBUTE_ID)
    private val secondName = commentAttributeName(FIRST_COMMENT_ATTRIBUTE_ID + 1)

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
        modifier = Modifier.fillMaxWidth()
    }

    @Test
    fun `should show a comment and the name of the attribute that gave it`() = runTest {
        val text = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(text to emptyList())),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretation(text)
            commentNamesShown() shouldBe listOf(firstName)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(createViewableInterpretation(), modifier = modifier, handler = handler)
            }
            requireInterpretation("")
            commentsShown() shouldBe emptyList()
        }
    }

    @Test
    fun `should show a row for each comment, each with its own name`() = runTest {
        val bondiComment = "Bondi."
        val malabarComment = "Malabar."
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(
                        mapOf(bondiComment to emptyList(), malabarComment to emptyList())
                    ),
                    modifier = modifier,
                    handler = handler
                )
            }
            commentsShown() shouldBe listOf(bondiComment, malabarComment)
            commentNamesShown() shouldBe listOf(firstName, secondName)
        }
    }

    // ==================== Hover ====================

    @Test
    fun `should highlight the row of the comment under the pointer`() = runTest {
        val bondiComment = "Bondi."
        val malabarComment = "Malabar."
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(
                        mapOf(bondiComment to emptyList(), malabarComment to emptyList())
                    ),
                    modifier = modifier,
                    handler = handler
                )
            }

            //When
            movePointerOverCommentRow(secondName)

            //Then only that row is highlighted
            requireCommentRowToBeHighlighted(secondName)
            requireCommentRowNotToBeHighlighted(firstName)
        }
    }

    @Test
    fun `should not highlight a comment if the pointer is not over it`() = runTest {
        val bondiComment = "Bondi."
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    modifier = modifier,
                    handler = handler
                )
            }
            movePointerOverCommentRow(firstName)
            requireCommentRowToBeHighlighted(firstName)

            //When
            movePointerAwayFromTheComments()

            //Then
            requireNoCommentRowToBeHighlighted()
        }
    }

    @Test
    fun `should show the conditions for the comment under the pointer`() = runTest {
        val bondiComment = "Best surf in the world!"
        val malabarComment = "Great for a swim!"
        val bondiConditions = listOf("Bring your flippers.", "And your sunscreeen.")
        val malabarConditions = listOf("Great for a swim!", "And a picnic.")
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(
                        mapOf(bondiComment to bondiConditions, malabarComment to malabarConditions)
                    ),
                    modifier = modifier,
                    handler = handler
                )
            }

            //When
            movePointerOverCommentRow(secondName)

            //Then the comments are still shown, with the conditions of the one hovered over
            requireConditionsToBeShowing(malabarConditions)
            commentsShown() shouldBe listOf(bondiComment, malabarComment)
        }
    }

    @Test
    fun `should show comment but not show any conditions for the comment under the pointer if there are none`() =
        runTest {
            val bondiComment = "Best surf in the world!"
            with(composeTestRule) {
                setContent {
                    ReadonlyInterpretationView(
                        createViewableInterpretation(mapOf(bondiComment to listOf())),
                        modifier = modifier,
                        handler = handler
                    )
                }

                //When
                movePointerOverCommentRow(firstName)

                //Then
                requireNoConditionsToBeShowing()
                requireInterpretation(bondiComment)
            }
        }

    @Test
    fun `should show comment but not show any conditions if the pointer is not over a comment`() = runTest {
        val bondiComment = "Best surf in the world!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to listOf("Sun is in case"))),
                    modifier = modifier,
                    handler = handler
                )
            }

            //When
            movePointerAwayFromTheComments()

            //Then
            requireNoConditionsToBeShowing()
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should not show change interpretation icon`() = runTest {
        val bondiComment = "Best surf in the world!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation = createViewableInterpretation(mapOf(bondiComment to listOf())),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretation(bondiComment)

            //Then
            requireChangeInterpretationIconToBeNotShowing()
        }
    }

    // ==================== Pending changes ====================

    @Test
    fun `should show an addition as a row after the comments of the case`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    diff = Addition(addedComment, "C99"),
                    modifier = modifier,
                    handler = handler
                )
            }

            //Then the comment being added is last, named, and marked as pending
            commentsShown() shouldBe listOf(bondiComment, addedComment)
            commentNamesShown() shouldBe listOf(firstName, "C99")
            onNodeWithContentDescription("$COMMENT_PENDING_ADD_PREFIX" + "C99", useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `should show an addition as the only row when the interpretation is blank`() = runTest {
        val addedComment = "Beach time!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(),
                    diff = Addition(addedComment, "C99"),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretation(addedComment)
            commentNamesShown() shouldBe listOf("C99")
        }
    }

    @Test
    fun `should show a removal with the comment still visible, marked as pending`() = runTest {
        val bondiComment = "Go to Bondi."
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    diff = Removal(bondiComment, firstName),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretation(bondiComment)
            onNodeWithContentDescription("$COMMENT_PENDING_REMOVE_PREFIX$firstName", useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun `should show a replacement as both comments in one row, each with its own name`() = runTest {
        val bondiComment = "Go to Bondi."
        val replacementComment = "Go to Maroubra."
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    diff = Replacement(bondiComment, replacementComment, "C99"),
                    modifier = modifier,
                    handler = handler
                )
            }

            //Then the comment going and the comment coming are both shown, each named
            commentsShown() shouldBe listOf(bondiComment, replacementComment)
            commentNamesShown() shouldBe listOf(firstName, "C99")
            onNodeWithContentDescription("$COMMENT_PENDING_REPLACE_PREFIX$firstName", useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun `should show the rule conditions when hovering over a comment being added`() = runTest {
        val addedComment = "Beach time!"
        val ruleConditions = listOf("UV is high", "Waves is high")
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(),
                    diff = Addition(addedComment, "C99"),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }

            //When
            movePointerOverPendingCommentRow(COMMENT_PENDING_ADD_PREFIX, "C99")

            //Then
            requireConditionsToBeShowing(ruleConditions)
        }
    }

    @Test
    fun `should show the rule conditions when hovering over a comment being removed`() = runTest {
        val bondiComment = "Go to Bondi."
        val ruleConditions = listOf("UV is high")
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    diff = Removal(bondiComment, firstName),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }

            //When
            movePointerOverPendingCommentRow(COMMENT_PENDING_REMOVE_PREFIX, firstName)

            //Then the conditions of the rule being built are shown, the comment
            //having none of its own
            requireConditionsToBeShowing(ruleConditions)
        }
    }

    @Test
    fun `should show the rule conditions when hovering over the comment that is replacing another`() = runTest {
        val bondiComment = "Go to Bondi."
        val replacementComment = "Go to Maroubra."
        val ruleConditions = listOf("UV is high", "Waves is high")
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    diff = Replacement(bondiComment, replacementComment, "C99"),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }

            //When
            movePointerOverReplacementHalf("C99")

            //Then
            requireConditionsToBeShowing(ruleConditions)
        }
    }

    @Test
    fun `should not show the rule conditions when hovering over a comment that is not being changed`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val ruleConditions = listOf("UV is high")
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(bondiComment to emptyList())),
                    diff = Addition(addedComment, "C99"),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }

            //When the comment that is staying is hovered over
            movePointerOverCommentRow(firstName)

            //Then the conditions of the rule being built are not shown, as they
            //are not the conditions that gave this comment
            requireNoConditionsToBeShowing()
        }
    }

    // ==================== Unresolved variable marker ====================

    @Test
    fun `should highlight an unresolved variable marker within the comment`() = runTest {
        val comment = "Glucose is {Glucose: no value} mmol/L"
        val marker = "{Glucose: no value}"
        val markerStart = comment.indexOf(marker)
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretationWithUnresolvedMarker(
                        comment,
                        listOf(IntRangeData(markerStart, markerStart + marker.length - 1))
                    ),
                    modifier = modifier,
                    handler = handler
                )
            }

            //Then the marker, and only the marker, is highlighted
            val span = commentTextStyles(firstName).single { it.item.background == UNRESOLVED_COLOR }
            span.start shouldBe markerStart
            span.end shouldBe markerStart + marker.length
        }
    }

    @Test
    fun `should not highlight anything in a comment with no unresolved variable`() = runTest {
        val comment = "Glucose is 5 mmol/L"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretationWithUnresolvedMarker(comment, emptyList()),
                    modifier = modifier,
                    handler = handler
                )
            }

            commentTextStyles(firstName).none { it.item.background == UNRESOLVED_COLOR } shouldBe true
        }
    }

    @Test
    fun `UnresolvedVariableTooltip should display the explanatory message`() = runTest {
        with(composeTestRule) {
            setContent {
                UnresolvedVariableTooltip()
            }
            onNodeWithContentDescription(UNRESOLVED_VARIABLE_TOOLTIP).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the unresolved variable tooltip when hovering over an unresolved marker`() = runTest {
        //Given a comment whose unresolved marker is at its start
        val marker = "{Glucose: no value}"
        val comment = "$marker was the glucose reading for this case"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretationWithUnresolvedMarker(comment, listOf(IntRangeData(0, marker.length - 1))),
                    modifier = modifier,
                    handler = handler
                )
            }

            //When the start of the comment, which is the marker, is hovered over
            movePointerOverStartOfComment(firstName)

            //Then
            waitUntilAsserted {
                onNodeWithContentDescription(UNRESOLVED_VARIABLE_TOOLTIP).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `should not show the unresolved variable tooltip when hovering over resolved text`() = runTest {
        //Given a comment whose unresolved marker is at its end
        val comment = "Glucose is a very long way from the marker at {Glucose: no value}"
        val marker = "{Glucose: no value}"
        val markerStart = comment.indexOf(marker)
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretationWithUnresolvedMarker(
                        comment,
                        listOf(IntRangeData(markerStart, markerStart + marker.length - 1))
                    ),
                    modifier = modifier,
                    handler = handler
                )
            }

            //When the start of the comment, which is resolved, is hovered over
            movePointerOverStartOfComment(firstName)

            //Then
            onNodeWithContentDescription(UNRESOLVED_VARIABLE_TOOLTIP).assertDoesNotExist()
        }
    }

    private fun interpretationWithUnresolvedMarker(
        commentText: String,
        unresolvedRanges: List<IntRangeData>
    ): ViewableInterpretation {
        val interp = createInterpretation(mapOf(commentText to emptyList()))
        val renderedComments = listOf(
            RenderedComment(text = commentText, unresolvedRanges = unresolvedRanges, name = firstName)
        )
        return ViewableInterpretation(
            interpretation = interp,
            textGivenByRules = commentText,
            renderedComments = renderedComments
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    //Given
    val bondiComment = "Best surf in the world!"
    val malabarComment = "Great for a swim!"
    val bondiConditions = listOf("Bring your flippers.", "And your sunscreeen.")
    val malabarConditions = listOf("Great for a swim!", "And a picnic.")
    val interpretation = createViewableInterpretation(
        mapOf(
            bondiComment to bondiConditions,
            malabarComment to malabarConditions
        )
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ReadonlyInterpretationView(interpretation, modifier = Modifier, handler = mockk())
        }
    }
}
