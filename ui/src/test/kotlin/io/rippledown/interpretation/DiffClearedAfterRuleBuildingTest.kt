package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.interpretation.COMMENT_PENDING_ADD_PREFIX
import io.rippledown.constants.interpretation.COMMENT_PENDING_REMOVE_PREFIX
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.utils.FIRST_COMMENT_ATTRIBUTE_ID
import io.rippledown.utils.commentAttributeName
import io.rippledown.utils.createViewableInterpretation
import io.rippledown.utils.waitUntilAsserted
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * Tests that verify the pending change is properly cleared from the Comments
 * table after rule building completes. These tests reproduce the bug where the
 * pointerInput coroutine in AnnotatedTextView captured a stale handler
 * reference, causing the change to persist on hover after the diff parameter
 * changed to null.
 */
@ExperimentalFoundationApi
class DiffClearedAfterRuleBuildingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: ReadonlyInterpretationViewHandler
    lateinit var modifier: Modifier

    private val clearDiffButtonTag = "clearDiffButton"
    private val firstName = commentAttributeName(FIRST_COMMENT_ATTRIBUTE_ID)
    private val secondName = commentAttributeName(FIRST_COMMENT_ATTRIBUTE_ID + 1)
    private val pendingName = "C99"

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
        modifier = Modifier.fillMaxWidth()
    }

    @Test
    fun `should not show an addition on hover after the change is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Addition(addedComment, pendingName),
                    modifier = modifier,
                    handler = handler
                )
            }
            // Initially shows the comment and the one being added
            requireInterpretation("$bondiComment $addedComment")

            // Clear the change (simulates rule completion)
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            // Now shows only the comment, with no row previewing a change
            requireInterpretation(bondiComment)
            requireNoPendingCommentRows()

            // Hovering over the comment does not bring the addition back
            movePointerOverCommentRow(firstName)
            requireInterpretation(bondiComment)
            requireNoPendingCommentRows()
        }
    }

    @Test
    fun `should not show a removal on hover after the change is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Removal(bondiComment, firstName),
                    modifier = modifier,
                    handler = handler
                )
            }
            // The comment is shown as being removed
            onNodeWithContentDescription("$COMMENT_PENDING_REMOVE_PREFIX$firstName", useUnmergedTree = true)
                .assertExists()

            // Clear the change
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            requireInterpretation(bondiComment)
            requireNoPendingCommentRows()

            // Hovering over the comment does not mark it as being removed again
            movePointerOverCommentRow(firstName)
            requireNoPendingCommentRows()
        }
    }

    @Test
    fun `should not show a replacement on hover after the change is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val replacementComment = "Go to Maroubra."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Replacement(bondiComment, replacementComment, pendingName),
                    modifier = modifier,
                    handler = handler
                )
            }
            // Initially shows the comment going and the one coming in its place
            requireInterpretation("$bondiComment $replacementComment")

            // Clear the change
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            // Now shows only the comment
            requireInterpretation(bondiComment)
            requireNoPendingCommentRows()

            // Hovering over the comment does not bring the replacement back
            movePointerOverCommentRow(firstName)
            requireInterpretation(bondiComment)
            requireNoPendingCommentRows()
        }
    }

    @Test
    fun `should highlight the row under the pointer after an addition is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        val diffState = mutableStateOf<Diff?>(Addition(addedComment, pendingName))

        with(composeTestRule) {
            setContent {
                DiffStateView(
                    interpretation = interpretation,
                    diffState = diffState,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretation("$bondiComment $addedComment")

            // Hover over the comment being added, to establish the pointer state
            movePointerOverPendingCommentRow(COMMENT_PENDING_ADD_PREFIX, pendingName)

            // Clear the change programmatically (simulates WebSocket rule completion)
            runOnIdle { diffState.value = null }
            waitForIdle()

            // Move the pointer to the remaining comment
            movePointerOverCommentRow(firstName)

            // Its row has the ordinary hover highlight, and nothing is pending
            waitUntilAsserted {
                requireCommentRowToBeHighlighted(firstName)
            }
            requireNoPendingCommentRows()
        }
    }

    @Test
    fun `should not show the rule conditions on hover after the change is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val ruleConditions = listOf("UV is high", "Waves is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Addition(addedComment, pendingName),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            // Hovering over the comment being added shows the conditions of the rule
            movePointerOverPendingCommentRow(COMMENT_PENDING_ADD_PREFIX, pendingName)
            requireConditionsToBeShowing(ruleConditions)

            // Move the pointer away and clear the change (simulates rule completion)
            movePointerAwayFromTheComments()
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            // Hovering over the comment that has no conditions of its own shows none
            movePointerOverCommentRow(firstName)
            requireNoConditionsToBeShowing()
        }
    }

    @Test
    fun `should show a comment's own conditions after the change is cleared`() = runTest {
        val bondiComment = "Best surf in the world!"
        val addedComment = "Beach time!"
        val bondiConditions = listOf("Bring your flippers.", "And your sunscreen.")
        val ruleConditions = listOf("UV is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to bondiConditions))
        val diffState = mutableStateOf<Diff?>(Addition(addedComment, pendingName))

        with(composeTestRule) {
            setContent {
                DiffStateView(
                    interpretation = interpretation,
                    diffState = diffState,
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretation("$bondiComment $addedComment")

            // Hover over the comment, to establish the pointer state
            movePointerOverCommentRow(firstName)

            // Clear the change programmatically (simulates WebSocket rule completion)
            runOnIdle { diffState.value = null }
            waitForIdle()

            // Hovering over the comment shows the conditions of the rule that gave it
            movePointerOverCommentRow(firstName)
            requireConditionsToBeShowing(bondiConditions)
        }
    }

    @Test
    fun `should show the comment as its own row when the change is cleared and the interpretation updates`() =
        runTest {
            val bondiComment = "Go to Bondi."
            val addedComment = "Beach time!"
            val originalInterpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
            val updatedInterpretation =
                createViewableInterpretation(mapOf(bondiComment to emptyList(), addedComment to emptyList()))

            with(composeTestRule) {
                setContent {
                    DiffAndInterpretationToggleView(
                        originalInterpretation = originalInterpretation,
                        updatedInterpretation = updatedInterpretation,
                        initialDiff = Addition(addedComment, pendingName),
                        modifier = modifier,
                        handler = handler
                    )
                }
                // Initially: the existing comment, and the one being added
                requireInterpretation("$bondiComment $addedComment")
                commentNamesShown() shouldBe listOf(firstName, pendingName)

                // Simulate rule completion: the change is cleared and the
                // interpretation now includes the comment the rule added
                onNodeWithTag(clearDiffButtonTag).performClick()
                waitForIdle()

                // Both comments are shown, and the added one is now a comment of
                // the case, named for its own attribute rather than as pending
                requireInterpretation("$bondiComment $addedComment")
                commentNamesShown() shouldBe listOf(firstName, secondName)
                requireNoPendingCommentRows()

                // Hovering over the first comment leaves both showing
                movePointerOverCommentRow(firstName)
                requireInterpretation("$bondiComment $addedComment")
                requireNoPendingCommentRows()
            }
        }

    @Composable
    private fun DiffToggleView(
        interpretation: ViewableInterpretation,
        initialDiff: Diff,
        ruleConditions: List<String> = emptyList(),
        modifier: Modifier,
        handler: ReadonlyInterpretationViewHandler
    ) {
        var diff: Diff? by remember { mutableStateOf(initialDiff) }

        ReadonlyInterpretationView(
            interpretation = interpretation,
            diff = diff,
            ruleConditions = ruleConditions,
            modifier = modifier,
            handler = handler
        )

        Button(
            onClick = { diff = null },
            modifier = Modifier.testTag(clearDiffButtonTag)
        ) {}
    }

    @Composable
    private fun DiffStateView(
        interpretation: ViewableInterpretation,
        diffState: MutableState<Diff?>,
        ruleConditions: List<String> = emptyList(),
        modifier: Modifier,
        handler: ReadonlyInterpretationViewHandler
    ) {
        ReadonlyInterpretationView(
            interpretation = interpretation,
            diff = diffState.value,
            ruleConditions = ruleConditions,
            modifier = modifier,
            handler = handler
        )
    }

    @Composable
    private fun DiffAndInterpretationToggleView(
        originalInterpretation: ViewableInterpretation,
        updatedInterpretation: ViewableInterpretation,
        initialDiff: Diff,
        modifier: Modifier,
        handler: ReadonlyInterpretationViewHandler
    ) {
        var diff: Diff? by remember { mutableStateOf(initialDiff) }
        var interpretation by remember { mutableStateOf(originalInterpretation) }

        ReadonlyInterpretationView(
            interpretation = interpretation,
            diff = diff,
            modifier = modifier,
            handler = handler
        )

        Button(
            onClick = {
                diff = null
                interpretation = updatedInterpretation
            },
            modifier = Modifier.testTag(clearDiffButtonTag)
        ) {}
    }
}
