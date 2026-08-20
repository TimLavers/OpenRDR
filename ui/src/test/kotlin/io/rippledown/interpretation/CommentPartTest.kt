@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.UNRESOLVED_VARIABLE_TOOLTIP
import io.rippledown.model.IntRangeData
import io.rippledown.model.RenderedComment
import io.rippledown.utils.waitUntilAsserted
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

/**
 * Tests of one part of a row of the Comments table: a name chip and the comment
 * beside it. A whole row is one part; a row previewing a replacement is two.
 */
class CommentPartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val nameId = "the name"
    private val textId = "the text"

    /**
     * The part is laid out within a row, being a [androidx.compose.foundation.layout.RowScope]
     * extension, so that its halves can be weighted against one another.
     */
    private fun ComposeContentTestRule.showPart(
        comment: RenderedComment,
        partBackground: Color = Color.Transparent,
        nameWeight: Float = 0.2f,
        partWeight: Float = 1f,
        onHoverChanged: (Boolean) -> Unit = {}
    ) {
        setContent {
            Row {
                CommentPart(
                    comment = comment,
                    partBackground = partBackground,
                    textDescription = textId,
                    nameDescription = nameId,
                    nameWeight = nameWeight,
                    partWeight = partWeight,
                    onHoverChanged = onHoverChanged
                )
            }
        }
    }

    @Test
    fun `should show the comment and the name of the attribute that gave it`() = runTest {
        val comment = RenderedComment(text = "Go to Bondi.", name = "C1")
        with(composeTestRule) {
            showPart(comment)

            onNodeWithContentDescription(nameId, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(comment.name)
            onNodeWithContentDescription(textId, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(comment.text)
        }
    }

    @Test
    fun `should show a comment that has no name`() = runTest {
        //Given a comment from a knowledge base whose comments are not named
        val comment = RenderedComment(text = "Go to Bondi.")
        with(composeTestRule) {
            showPart(comment)

            //Then the comment is shown, with an empty name beside it
            onNodeWithContentDescription(textId, useUnmergedTree = true).assertTextEquals(comment.text)
            onNodeWithContentDescription(nameId, useUnmergedTree = true).assertTextEquals("")
        }
    }

    // ==================== Unresolved variable markers ====================

    @Test
    fun `should highlight an unresolved variable marker within the comment`() = runTest {
        val marker = "{Glucose: no value}"
        val text = "Glucose is $marker mmol/L"
        val markerStart = text.indexOf(marker)
        val comment = RenderedComment(
            text = text,
            unresolvedRanges = listOf(IntRangeData(markerStart, markerStart + marker.length - 1)),
            name = "C1"
        )
        with(composeTestRule) {
            showPart(comment)

            //Then the marker, and only the marker, is highlighted
            val span = textStyles().single { it.item.background == UNRESOLVED_COLOR }
            span.start shouldBe markerStart
            span.end shouldBe markerStart + marker.length
        }
    }

    @Test
    fun `should highlight each of several unresolved variable markers`() = runTest {
        val text = "{A: no value} and {B: no value}"
        val comment = RenderedComment(
            text = text,
            unresolvedRanges = listOf(
                IntRangeData(0, "{A: no value}".length - 1),
                IntRangeData(text.indexOf("{B"), text.length - 1)
            ),
            name = "C1"
        )
        with(composeTestRule) {
            showPart(comment)

            textStyles().count { it.item.background == UNRESOLVED_COLOR } shouldBe 2
        }
    }

    @Test
    fun `should not highlight anything in a comment with no unresolved variable`() = runTest {
        val comment = RenderedComment(text = "Glucose is 5 mmol/L", name = "C1")
        with(composeTestRule) {
            showPart(comment)

            textStyles().none { it.item.background == UNRESOLVED_COLOR } shouldBe true
        }
    }

    // ==================== Hover ====================

    @Test
    fun `should report the pointer entering and leaving the comment`() = runTest {
        val comment = RenderedComment(text = "Go to Bondi.", name = "C1")
        val hovers = mutableListOf<Boolean>()
        with(composeTestRule) {
            showPart(comment, onHoverChanged = { hovers.add(it) })

            //When the pointer moves over the comment and then away
            onNodeWithContentDescription(textId, useUnmergedTree = true)
                .performMouseInput { moveTo(center) }
            waitForIdle()
            waitUntilAsserted { hovers.last() shouldBe true }

            onNodeWithContentDescription(textId, useUnmergedTree = true)
                .performMouseInput { moveTo(Offset(-10f, -10f)) }
            waitForIdle()

            //Then both the entry and the exit are reported, so that the row can
            //highlight itself and then stop
            waitUntilAsserted { hovers.last() shouldBe false }
        }
    }

    @Test
    fun `should show the conditions of the rule that gave the comment when hovered over`() = runTest {
        val conditions = listOf("Sex is F", "Age is high")
        val comment = RenderedComment(text = "Go to Bondi.", conditions = conditions, name = "C1")
        with(composeTestRule) {
            showPart(comment)

            //When
            hoverOverTheComment()

            //Then
            requireConditionsToBeShowing(conditions)
        }
    }

    @Test
    fun `should show no conditions for a comment that has none`() = runTest {
        val comment = RenderedComment(text = "Go to Bondi.", name = "C1")
        with(composeTestRule) {
            showPart(comment)

            //When
            hoverOverTheComment()

            //Then
            requireNoConditionsToBeShowing()
        }
    }

    @Test
    fun `should explain an unresolved variable marker when it is hovered over`() = runTest {
        //Given a comment whose unresolved marker is at its start
        val marker = "{Glucose: no value}"
        val comment = RenderedComment(
            text = "$marker was the reading for this case",
            unresolvedRanges = listOf(IntRangeData(0, marker.length - 1)),
            name = "C1"
        )
        with(composeTestRule) {
            showPart(comment)

            //When the start of the comment, which is the marker, is hovered over
            onNodeWithContentDescription(textId, useUnmergedTree = true)
                .performMouseInput { moveTo(Offset(2f, center.y)) }
            waitForIdle()

            //Then the marker is explained, in place of the conditions
            waitUntilAsserted {
                onNodeWithContentDescription(UNRESOLVED_VARIABLE_TOOLTIP).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `should show the conditions rather than the explanation when resolved text is hovered over`() = runTest {
        //Given a comment whose unresolved marker is at its end
        val marker = "{Glucose: no value}"
        val conditions = listOf("Sex is F")
        val text = "The glucose reading for this case was a long way from $marker"
        val comment = RenderedComment(
            text = text,
            conditions = conditions,
            unresolvedRanges = listOf(IntRangeData(text.indexOf(marker), text.length - 1)),
            name = "C1"
        )
        with(composeTestRule) {
            showPart(comment)

            //When the start of the comment, which is resolved, is hovered over
            onNodeWithContentDescription(textId, useUnmergedTree = true)
                .performMouseInput { moveTo(Offset(2f, center.y)) }
            waitForIdle()

            //Then
            requireConditionsToBeShowing(conditions)
            onNodeWithContentDescription(UNRESOLVED_VARIABLE_TOOLTIP).assertDoesNotExist()
        }
    }

    private fun ComposeTestRule.hoverOverTheComment() {
        onNodeWithContentDescription(textId, useUnmergedTree = true)
            .performMouseInput { moveTo(center) }
        waitForIdle()
    }

    private fun ComposeTestRule.textStyles() =
        onNodeWithContentDescription(textId, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .flatMap { it.spanStyles }
}
