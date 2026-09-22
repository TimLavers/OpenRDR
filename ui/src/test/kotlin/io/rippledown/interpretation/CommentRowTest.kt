@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.*
import io.rippledown.model.RenderedComment
import io.rippledown.utils.asConditionTexts
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

/**
 * Tests of one row of the Comments table. The rows to show are worked out by
 * [commentRowsToDisplay], which [CommentRowsTest] covers; these tests are of
 * how a row that has been worked out is shown.
 */
class CommentRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bondi = RenderedComment(
        text = "Go to Bondi.",
        conditions = listOf("Sex is F").asConditionTexts(),
        name = "C1"
    )
    private val malabar = RenderedComment(text = "Go to Malabar.", name = "C2")

    @Test
    fun `should show the comment and its name`() = runTest {
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi)) }

            onNodeWithContentDescription("$COMMENT_ROW_PREFIX${bondi.name}").assertExists()
            onNodeWithContentDescription("$COMMENT_NAME_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.name)
            onNodeWithContentDescription("$COMMENT_TEXT_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.text)
        }
    }

    /**
     * The pending change a row previews is carried by the id of its text, since
     * the tinting that shows it on screen is invisible to the semantics tree.
     */
    @Test
    fun `should mark the text of a comment being added`() = runTest {
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi, CommentHighlight.ADDED)) }

            onNodeWithContentDescription("$COMMENT_PENDING_ADD_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.text)
            onNodeWithContentDescription("$COMMENT_TEXT_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `should mark the text of a comment being removed`() = runTest {
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi, CommentHighlight.REMOVED)) }

            onNodeWithContentDescription("$COMMENT_PENDING_REMOVE_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.text)
        }
    }

    // ==================== A replacement ====================

    @Test
    fun `should show a replacement as both comments in the one row, each with its own name`() = runTest {
        // Given a replacement preview constrained to the width of the Comments panel
        val replacement = malabar.copy(conditions = listOf("Sun is in case").asConditionTexts())
        with(composeTestRule) {
            setContent {
                Box(Modifier.width(500.dp)) {
                    CommentRow(CommentRowState(bondi, CommentHighlight.REPLACED, replacement = replacement))
                }
            }

            //The comment going, marked as being replaced
            onNodeWithContentDescription("$COMMENT_PENDING_REPLACE_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.text)
            onNodeWithContentDescription("$COMMENT_NAME_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.name)

            //And the comment coming in its place, beside it
            onNodeWithContentDescription("$COMMENT_REPLACEMENT_TEXT_PREFIX${malabar.name}", useUnmergedTree = true)
                .assertTextEquals(malabar.text)
            onNodeWithContentDescription("$COMMENT_REPLACEMENT_NAME_PREFIX${malabar.name}", useUnmergedTree = true)
                .assertTextEquals(malabar.name)

            // Then both text cells have usable width inside their own half of the row
            val rowBounds = onNodeWithContentDescription("$COMMENT_ROW_PREFIX${bondi.name}")
                .fetchSemanticsNode().boundsInRoot
            val oldText = onNodeWithContentDescription("$COMMENT_PENDING_REPLACE_PREFIX${bondi.name}")
            val newText = onNodeWithContentDescription("$COMMENT_REPLACEMENT_TEXT_PREFIX${malabar.name}")
            val oldBounds = oldText.fetchSemanticsNode().boundsInRoot
            val newBounds = newText.fetchSemanticsNode().boundsInRoot
            withClue("Both replacement cells must fit side by side: row=$rowBounds, old=$oldBounds, new=$newBounds") {
                (oldBounds.width > rowBounds.width * 0.3f && oldBounds.width < rowBounds.width * 0.5f) shouldBe true
                (newBounds.width > rowBounds.width * 0.3f && newBounds.width < rowBounds.width * 0.5f) shouldBe true
                (oldBounds.right <= newBounds.left && newBounds.right <= rowBounds.right) shouldBe true
            }

            // When hovering the comment coming in place of the original
            newText.performMouseInput { moveTo(center) }

            // Then the rule being built supplies its conditions
            requireConditionsToBeShowing(listOf("Sun is in case"))
            onNodeWithContentDescription("${CONDITION_PREFIX}Sex is F").assertDoesNotExist()
        }
    }

    @Test
    fun `should show the comment going and the one coming in the one row`() = runTest {
        with(composeTestRule) {
            setContent {
                CommentRow(CommentRowState(bondi, CommentHighlight.REPLACED, replacement = malabar))
            }

            //Both halves are within the single row, which is named for the
            //comment being replaced
            onAllNodesWithContentDescription(COMMENT_ROW_PREFIX, substring = true).assertCountEquals(1)
            onNodeWithContentDescription("$COMMENT_ROW_PREFIX${bondi.name}").assertExists()
        }
    }

    /**
     * A row marked as a replacement but with no replacement to show would be a
     * mistake in the rows worked out for the table; the row shows the comment
     * it has rather than failing.
     */
    @Test
    fun `should show a single comment when a replacement is marked but not given`() = runTest {
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi, CommentHighlight.REPLACED)) }

            onNodeWithContentDescription("$COMMENT_PENDING_REPLACE_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.text)
            onAllNodesWithContentDescription(COMMENT_REPLACEMENT_TEXT_PREFIX, substring = true).assertCountEquals(0)
        }
    }

    // ==================== Hover ====================

    @Test
    fun `should highlight the row when the pointer is over the comment`() = runTest {
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi)) }
            onNodeWithTag("$COMMENT_ROW_TAG_PREFIX${bondi.name}").assertExists()

            //When
            onNodeWithContentDescription("$COMMENT_TEXT_PREFIX${bondi.name}", useUnmergedTree = true)
                .performMouseInput { moveTo(center) }
            waitForIdle()

            //Then
            onNodeWithTag("$COMMENT_ROW_HOVERED_TAG_PREFIX${bondi.name}").assertExists()
        }
    }

    @Test
    fun `should stop highlighting the row when the pointer leaves the comment`() = runTest {
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi)) }
            onNodeWithContentDescription("$COMMENT_TEXT_PREFIX${bondi.name}", useUnmergedTree = true)
                .performMouseInput { moveTo(center) }
            waitForIdle()
            onNodeWithTag("$COMMENT_ROW_HOVERED_TAG_PREFIX${bondi.name}").assertExists()

            //When
            onNodeWithContentDescription("$COMMENT_TEXT_PREFIX${bondi.name}", useUnmergedTree = true)
                .performMouseInput { moveTo(Offset(-10f, -10f)) }
            waitForIdle()

            //Then
            onNodeWithTag("$COMMENT_ROW_TAG_PREFIX${bondi.name}").assertExists()
            onNodeWithTag("$COMMENT_ROW_HOVERED_TAG_PREFIX${bondi.name}").assertDoesNotExist()
        }
    }

    /**
     * Either half of a replacement highlights the row, the two being the one row.
     */
    @Test
    fun `should highlight the row when the pointer is over the comment coming in place of another`() = runTest {
        with(composeTestRule) {
            setContent {
                CommentRow(CommentRowState(bondi, CommentHighlight.REPLACED, replacement = malabar))
            }

            //When the half showing the replacement is hovered over
            onNodeWithContentDescription("$COMMENT_REPLACEMENT_TEXT_PREFIX${malabar.name}", useUnmergedTree = true)
                .performMouseInput { moveTo(center) }
            waitForIdle()

            //Then the row, which is named for the comment being replaced, is highlighted
            onNodeWithTag("$COMMENT_ROW_HOVERED_TAG_PREFIX${bondi.name}").assertExists()
        }
    }

    // ==================== Several tables on screen ====================

    @Test
    fun `should prefix its ids so that two tables on screen can be told apart`() = runTest {
        val prefix = CORNERSTONE_COMMENT_ID_PREFIX
        with(composeTestRule) {
            setContent { CommentRow(CommentRowState(bondi), idPrefix = prefix) }

            onNodeWithContentDescription("$prefix$COMMENT_ROW_PREFIX${bondi.name}").assertExists()
            onNodeWithContentDescription("$prefix$COMMENT_NAME_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.name)
            onNodeWithContentDescription("$prefix$COMMENT_TEXT_PREFIX${bondi.name}", useUnmergedTree = true)
                .assertTextEquals(bondi.text)
            onNodeWithTag("$prefix$COMMENT_ROW_TAG_PREFIX${bondi.name}").assertExists()
        }
    }
}
