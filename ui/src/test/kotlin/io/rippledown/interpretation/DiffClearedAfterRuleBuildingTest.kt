package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.utils.createViewableInterpretation
import io.rippledown.utils.waitUntilAsserted
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * Tests that verify the diff is properly cleared after rule building completes.
 * These tests reproduce the bug where the pointerInput coroutine in AnnotatedTextView
 * captured a stale handler reference, causing the diff to persist on hover after
 * the diff parameter changed to null.
 */
@ExperimentalFoundationApi
class DiffClearedAfterRuleBuildingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: ReadonlyInterpretationViewHandler
    lateinit var modifier: Modifier

    private val clearDiffButtonTag = "clearDiffButton"

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
        modifier = Modifier.fillMaxWidth()
    }

    @Test
    fun `should not show addition diff on hover after diff is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Addition(addedComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            // Initially shows comment + diff
            requireInterpretationForCornerstone("$bondiComment $addedComment")

            // Clear the diff (simulates rule completion)
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            // Now should show only the comment without diff
            requireInterpretationForCornerstone(bondiComment)

            // Hover over the comment - should still show only the comment, no diff
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()
            requireInterpretationForCornerstone(bondiComment)
        }
    }

    @Test
    fun `should not show removal diff on hover after diff is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Removal(bondiComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(bondiComment)

            // Clear the diff
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            requireInterpretationForCornerstone(bondiComment)

            // Hover over the comment - should not show removal styling
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()
            requireInterpretationForCornerstone(bondiComment)
        }
    }

    @Test
    fun `should not show replacement diff on hover after diff is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val replacementComment = "Go to Maroubra."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Replacement(bondiComment, replacementComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            // Initially shows original + replacement
            requireInterpretationForCornerstone("$bondiComment $replacementComment")

            // Clear the diff
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            // Now should show only the comment without replacement
            requireInterpretationForCornerstone(bondiComment)

            // Hover - should show only the original comment
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()
            requireInterpretationForCornerstone(bondiComment)
        }
    }

    @Test
    fun `should show correct highlight on hover after addition diff is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        val diffState = mutableStateOf<Diff?>(Addition(addedComment))

        with(composeTestRule) {
            setContent {
                DiffStateView(
                    interpretation = interpretation,
                    diffState = diffState,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone("$bondiComment $addedComment")

            // Hover over diff text to establish pointer state at a different position
            movePointerOverComment(addedComment, textLayoutResult)
            waitForIdle()

            // Clear diff programmatically (simulates WebSocket rule completion)
            runOnIdle { diffState.value = null }
            waitForIdle()

            // Move pointer to comment (different position from where it was)
            movePointerOverComment(bondiComment, textLayoutResult)

            // Should have normal hover highlight, not diff colours
            waitUntilAsserted {
                requireCommentToBeHighlighted(bondiComment, textLayoutResult)
            }
        }
    }

    @Test
    fun `should not show rule conditions tooltip on hover after diff is cleared`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val ruleConditions = listOf("UV is high", "Waves is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }

        with(composeTestRule) {
            setContent {
                DiffToggleView(
                    interpretation = interpretation,
                    initialDiff = Addition(addedComment),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone("$bondiComment $addedComment")

            // Hover over diff text - should show rule conditions
            movePointerOverComment(addedComment, textLayoutResult)
            requireConditionsToBeShowing(ruleConditions)

            // Move pointer away
            movePointerBelowTheText(textLayoutResult)
            waitForIdle()

            // Clear the diff (simulates rule completion)
            onNodeWithTag(clearDiffButtonTag).performClick()
            waitForIdle()

            // Hover over the regular comment - should NOT show rule conditions
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()
            requireNoConditionsToBeShowing()
        }
    }

    @Test
    fun `should show comment conditions tooltip after diff is cleared`() = runTest {
        val bondiComment = "Best surf in the world!"
        val addedComment = "Beach time!"
        val bondiConditions = listOf("Bring your flippers.", "And your sunscreen.")
        val ruleConditions = listOf("UV is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to bondiConditions))
        var textLayoutResult: TextLayoutResult? = null
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        val diffState = mutableStateOf<Diff?>(Addition(addedComment))

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
            requireInterpretationForCornerstone("$bondiComment $addedComment")

            // Hover over comment to establish pointer state
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()

            // Clear diff programmatically (simulates WebSocket rule completion)
            runOnIdle { diffState.value = null }
            waitForIdle()

            // Move pointer to trigger handler with updated diff
            movePointerOverComment(bondiComment, textLayoutResult)
            requireConditionsToBeShowing(bondiConditions)
        }
    }

    @Test
    fun `should update text correctly when diff changes from addition to null and interpretation also updates`() =
        runTest {
            val bondiComment = "Go to Bondi."
            val addedComment = "Beach time!"
            val originalInterpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
            val updatedInterpretation =
                createViewableInterpretation(mapOf(bondiComment to emptyList(), addedComment to emptyList()))
            var textLayoutResult: TextLayoutResult? = null
            val handler = object : ReadonlyInterpretationViewHandler by handler {
                override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                    textLayoutResult = layoutResult
                }
            }

            with(composeTestRule) {
                setContent {
                    DiffAndInterpretationToggleView(
                        originalInterpretation = originalInterpretation,
                        updatedInterpretation = updatedInterpretation,
                        initialDiff = Addition(addedComment),
                        modifier = modifier,
                        handler = handler
                    )
                }
                // Initially: existing comment + diff addition
                requireInterpretationForCornerstone("$bondiComment $addedComment")

                // Simulate rule completion: diff cleared AND interpretation updated
                onNodeWithTag(clearDiffButtonTag).performClick()
                waitForIdle()

                // Should show both comments (diff is now a real part of interpretation)
                requireInterpretationForCornerstone("$bondiComment $addedComment")

                // Hover over the original comment - no green diff, just regular highlight
                movePointerOverComment(bondiComment, textLayoutResult)
                waitForIdle()
                requireInterpretationForCornerstone("$bondiComment $addedComment")

                // The added comment should not have diff styling - it's now a real comment
                val annotatedText = textLayoutResult.layoutInput.text
                val diffSpans = annotatedText.spanStyles.filter {
                    it.item.background == DIFF_ADDITION_COLOR || it.item.background == DIFF_REMOVAL_COLOR
                }
                diffSpans.size shouldBe 0
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
