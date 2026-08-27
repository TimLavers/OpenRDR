@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.interpretation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.*
import io.rippledown.decoration.BACKGROUND_COLOR
import io.rippledown.utils.dump
import io.rippledown.utils.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute


/**
 * The comments shown by the Comments table, in the order of its rows. A row
 * previewing a replacement contributes the comment being replaced followed by
 * the one replacing it, as both are shown in that row.
 */
fun ComposeTestRule.commentsShown(idPrefix: String = ""): List<String> =
    onAllNodes(isCommentText(idPrefix), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
        }

/**
 * The names shown in the name column, in row order. A row previewing a
 * replacement contributes the name of the comment being replaced followed by
 * that of the one replacing it.
 */
fun ComposeTestRule.commentNamesShown(idPrefix: String = ""): List<String> =
    onAllNodes(isCommentName(idPrefix), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""
        }

/**
 * The comments of the table read as one string, as they were shown before the
 * comments became a table, so that an expectation of the whole interpretation
 * can still be written as one string.
 */
fun ComposeTestRule.requireInterpretation(text: String) {
    commentsShown().joinToString(" ") shouldBe text
}

fun ComposeTestRule.requireInterpretationForCornerstone(text: String) {
    commentsShown(CORNERSTONE_COMMENT_ID_PREFIX).joinToString(" ") shouldBe text
}

fun ComposeTestRule.requireCommentNames(vararg names: String) {
    commentNamesShown() shouldBe names.toList()
}

/**
 * No row is previewing a pending change, which is how the table shows that the
 * rule session has finished or been cancelled.
 */
fun ComposeTestRule.requireNoPendingCommentRows() {
    onAllNodes(isPendingCommentText(), useUnmergedTree = true).assertCountEquals(0)
}

private fun isPendingCommentText() = SemanticsMatcher("is a comment being changed") { node ->
    node.hasDescriptionStartingWithAnyOf(
        COMMENT_PENDING_ADD_PREFIX,
        COMMENT_PENDING_REMOVE_PREFIX,
        COMMENT_PENDING_REPLACE_PREFIX,
        COMMENT_REPLACEMENT_TEXT_PREFIX
    )
}

fun ComposeTestRule.requireCommentShownFor(name: String, text: String) {
    onNode(hasContentDescriptionStartingWith("$COMMENT_TEXT_PREFIX$name"), useUnmergedTree = true)
        .assertTextEquals(text)
}

/**
 * The styles applied within the text of the comment given by the named comment
 * attribute, which is how the highlighting of an unresolved variable marker
 * within a comment can be told.
 */
fun ComposeTestRule.commentTextStyles(name: String, idPrefix: String = "") =
    onNode(hasContentDescriptionStartingWith("$idPrefix$COMMENT_TEXT_PREFIX$name"), useUnmergedTree = true)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .flatMap { it.spanStyles }

private fun isCommentText(idPrefix: String) = SemanticsMatcher("is a comment text cell") { node ->
    node.hasDescriptionStartingWithAnyOf(
        "$idPrefix$COMMENT_TEXT_PREFIX",
        "$idPrefix$COMMENT_PENDING_ADD_PREFIX",
        "$idPrefix$COMMENT_PENDING_REMOVE_PREFIX",
        "$idPrefix$COMMENT_PENDING_REPLACE_PREFIX",
        "$idPrefix$COMMENT_REPLACEMENT_TEXT_PREFIX"
    )
}

private fun isCommentName(idPrefix: String) = SemanticsMatcher("is a comment name chip") { node ->
    node.hasDescriptionStartingWithAnyOf(
        "$idPrefix$COMMENT_NAME_PREFIX",
        "$idPrefix$COMMENT_REPLACEMENT_NAME_PREFIX"
    )
}

private fun hasContentDescriptionStartingWith(prefix: String) =
    SemanticsMatcher("content description starts with $prefix") { node ->
        node.hasDescriptionStartingWithAnyOf(prefix)
    }

/**
 * Matching on the whole prefix, rather than on a substring, keeps the
 * cornerstone view's rows out of the case's, since its ids only differ by their
 * leading prefix.
 */
private fun SemanticsNode.hasDescriptionStartingWithAnyOf(vararg prefixes: String) =
    config.getOrNull(SemanticsProperties.ContentDescription)?.any { description ->
        prefixes.any { description.startsWith(it) }
    } == true

fun ComposeTestRule.requireChangeInterpretationIconToBeShowing() {
    onNodeWithContentDescription(CHANGE_INTERPRETATION_ICON).assertIsDisplayed()
}

fun ComposeTestRule.requireChangeInterpretationIconToBeNotShowing() {
    waitUntil {
        try {
            onNodeWithContentDescription(CHANGE_INTERPRETATION_ICON).assertDoesNotExist()
            true
        } catch (e: Throwable) {
            false
        }
    }
    onNodeWithContentDescription(CHANGE_INTERPRETATION_ICON).assertDoesNotExist()
}

fun ComposeTestRule.requireInterpretationActionsMenuToBeShowing() {
    onNodeWithContentDescription(CHANGE_INTERPRETATION_DROPDOWN).assertIsDisplayed()
}
fun ComposeTestRule.requireInterpretationActionsMenuToBeNotShowing() {
    onNodeWithContentDescription(CHANGE_INTERPRETATION_DROPDOWN).assertDoesNotExist()
}

fun ComposeTestRule.clickChangeInterpretationButton() {
    waitForIdle()
    onNodeWithContentDescription(CHANGE_INTERPRETATION_ICON)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickAddCommentMenu() {
    waitForIdle()
    waitTillButtonIsEnabled(ADD_COMMENT_MENU)
    onNodeWithContentDescription(ADD_COMMENT_MENU)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}


fun ComposeTestRule.clickReplaceCommentMenu() {
    waitForIdle()
    waitTillButtonIsEnabled(REPLACE_COMMENT_MENU)
    onNodeWithContentDescription(REPLACE_COMMENT_MENU)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickRemoveCommentMenu() {
    waitForIdle()
    waitTillButtonIsEnabled(REMOVE_COMMENT_MENU)
    onNodeWithContentDescription(REMOVE_COMMENT_MENU)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

private fun ComposeTestRule.enterCommentToBeAdded(comment: String) {
    onNodeWithContentDescription(ADD_COMMENT_TEXT_FIELD)
        .assertIsDisplayed()
        .performTextInput(comment)
}

fun ComposeTestRule.enterCommentToBeTheReplacement(comment: String) {
    onNodeWithContentDescription(REPLACEMENT_COMMENT_TEXT_FIELD)
        .assertIsDisplayed()
        .performTextInput(comment)
}

fun ComposeTestRule.addNewComment(comment: String) {
    enterCommentToBeAdded(comment)
    clickOKToAddNewComment()
}

fun ComposeTestRule.replaceComment(toBeReplaced: String, replacement: String) {
    clickCommentToBeReplaced(toBeReplaced)
    enterCommentToBeTheReplacement(replacement)
    clickOKToReplaceComment()
}

fun ComposeTestRule.removeComment(comment: String) {
    clickCommentToRemove(comment)
    clickOKToRemoveComment()
}

fun ComposeTestRule.clickCommentToRemove(comment: String) {
    clickComment(REMOVE_COMMENT_PREFIX, comment)
}

fun ComposeTestRule.clickCommentToBeReplaced(comment: String) {
    clickComment(REPLACED_COMMENT_PREFIX, comment)
}

fun ComposeTestRule.clickComment(prefix: String, comment: String) {
    execute {
        waitUntil { onNodeWithContentDescription("$prefix$comment").isDisplayed() }
        onNodeWithContentDescription("$prefix$comment").performClick()
        waitForIdle()

    }
}

fun ComposeTestRule.clickOKToAddNewComment() {
    waitForIdle()
    waitTillButtonIsEnabled(OK_BUTTON_FOR_ADD_COMMENT)
    waitForIdle()
    execute {
        onNodeWithContentDescription(OK_BUTTON_FOR_ADD_COMMENT)
            .assertIsDisplayed()
            .performClick()
        waitForIdle()
    }
}

fun ComposeTestRule.clickOKToReplaceComment() {
    waitForIdle()
    waitTillButtonIsEnabled(OK_BUTTON_FOR_REPLACE_COMMENT)
    waitForIdle()
    execute {
        onNodeWithContentDescription(OK_BUTTON_FOR_REPLACE_COMMENT)
            .performClick()
        waitForIdle()
    }
}

private fun ComposeTestRule.waitTillButtonIsEnabled(contentDescriptionForButton: String) {
    waitUntil(2_000) {
        try {
            onNodeWithContentDescription(contentDescriptionForButton).assertIsEnabled()
            true
        } catch (e: Throwable) {
            false
        }
    }
}

fun ComposeTestRule.clickOKToRemoveComment() {
    waitForIdle()
    waitTillButtonIsEnabled(OK_BUTTON_FOR_REMOVE_COMMENT)
    execute {
        onNodeWithContentDescription(OK_BUTTON_FOR_REMOVE_COMMENT)
            .performClick()
        waitForIdle()
    }
}

fun ComposeTestRule.requireOKButtonOnRemoveCommentDialogToBeDisabled() {
    onNodeWithContentDescription(OK_BUTTON_FOR_REMOVE_COMMENT)
        .assertIsNotEnabled()
}

fun ComposeTestRule.requireOKButtonOnReplaceCommentDialogToBeDisabled() {
    onNodeWithContentDescription(OK_BUTTON_FOR_REPLACE_COMMENT)
        .assertIsNotEnabled()
}

fun ComposeTestRule.clickCancelAddNewComment() {
    waitTillButtonIsEnabled(CANCEL_BUTTON_FOR_ADD_COMMENT)
    onNodeWithContentDescription(CANCEL_BUTTON_FOR_ADD_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickCancelReplaceComment() {
    waitTillButtonIsEnabled(CANCEL_BUTTON_FOR_REPLACE_COMMENT)
    onNodeWithContentDescription(CANCEL_BUTTON_FOR_REPLACE_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickCancelRemoveComment() {
    waitTillButtonIsEnabled(CANCEL_BUTTON_FOR_REMOVE_COMMENT)
    onNodeWithContentDescription(CANCEL_BUTTON_FOR_REMOVE_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.requireCommentSelectorLabel(expected: String) {
    onNodeWithContentDescription(COMMENT_SELECTOR_LABEL, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals(expected)
}

fun ComposeTestRule.requireCommentOptionsToBeDisplayed(prefix: String, options: List<String>) {
    options.forEach { option ->
        onNodeWithContentDescription(prefix + option, useUnmergedTree = true)
            .assertIsDisplayed()
    }
}

fun ComposeTestRule.requireCommentOptionsToExist(prefix: String, options: List<String>) {
    onNodeWithContentDescription("Options").dump()
    options.forEach { option ->
        println("CommentSelectorTest: looking for option $option")
        onNodeWithContentDescription(prefix + option, useUnmergedTree = true)
            .assertExists()
        println("CommentSelectorTest: found       option $option")
    }
}

fun ComposeTestRule.requireCommentOptionsNotToExist(prefix: String, options: List<String>) {
    options.forEach { option ->
        onNodeWithContentDescription("$prefix$option", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}

fun ComposeTestRule.requireCommentOptionsNotToBeDisplayed(prefix: String, options: List<String>) {
    options.forEach { option ->
        onNodeWithContentDescription("$prefix$option")
            .assertIsNotDisplayed()
    }
}

fun ComposeTestRule.enterTextIntoTheCommentSelector(prefix: String, text: String) {
    onNodeWithContentDescription(prefix + COMMENT_SELECTOR_TEXT_FIELD)
        .assertIsDisplayed()
        .performTextInput(text)
    waitForIdle()
}

fun ComposeTestRule.scrollToOption(prefix: String, option: String) {
    onNodeWithContentDescription("$prefix$option")
        .performScrollTo()
}

fun requireCommentToBeHighlighted(comment: String, layoutResult: TextLayoutResult) {
    requireStyleForCommentToHaveBackground(layoutResult, comment, BACKGROUND_COLOR)
}

fun requireCommentToBeNotHighlighted(layoutResult: TextLayoutResult) {
    layoutResult.layoutInput.text.spanStyles.size shouldBe 0
}

/**
 * The row of the comment given by the named comment attribute is highlighted,
 * which its tag reports, the highlight itself being a background that the
 * semantics tree does not show.
 */
fun ComposeTestRule.requireCommentRowToBeHighlighted(name: String, idPrefix: String = "") {
    onNodeWithTag("$idPrefix$COMMENT_ROW_HOVERED_TAG_PREFIX$name", useUnmergedTree = true).assertExists()
}

fun ComposeTestRule.requireCommentRowNotToBeHighlighted(name: String, idPrefix: String = "") {
    onNodeWithTag("$idPrefix$COMMENT_ROW_TAG_PREFIX$name", useUnmergedTree = true).assertExists()
}

fun ComposeTestRule.requireNoCommentRowToBeHighlighted() {
    onAllNodesWithTag(COMMENT_ROW_HOVERED_TAG_PREFIX, useUnmergedTree = true).assertCountEquals(0)
}

fun requireStyleForCommentToHaveBackground(layoutResult: TextLayoutResult, comment: String, color: Color) {
    val annotatedString = layoutResult.layoutInput.text
    requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, comment, color)
}

fun requireStyleForCommentInAnnotatedStringToHaveBackground(
    annotatedString: AnnotatedString,
    comment: String,
    color: Color
) {
    annotatedString.spanStyles.size shouldBe 1
    val startIndex = annotatedString.text.indexOf(comment)
    for (spanStyle in annotatedString.spanStyles) {
        if (startIndex == spanStyle.start) {
            withClue("check that background color is set for the first character of the comment") {
                spanStyle.item.background shouldBe color
            }

            withClue("check that the same style is used for all characters") {
                startIndex + comment.length shouldBeLessThanOrEqual spanStyle.end
            }
        }
    }
}

private fun ComposeTestRule.performMouseInput(action: MouseInjectionScope.() -> Unit) {
    onAllNodes(isRoot())[0].performMouseInput { action() }
}

fun ComposeTestRule.movePointerOverCharacter(charIndex: Int, layoutResult: TextLayoutResult) {
    performMouseInput {
        moveTo(absoluteCharacterPosition(layoutResult, charIndex))
    }
}

fun ComposeTestRule.movePointerToTheRightOfTheCharacter(charIndex: Int, layoutResult: TextLayoutResult) {
    performMouseInput {
        moveTo(absoluteCharacterPosition(layoutResult, charIndex) + Offset(10f, 0f))
    }
}

private fun ComposeTestRule.absoluteCharacterPosition(
    layoutResult: TextLayoutResult,
    charIndex: Int
): Offset {
    val allTextInLayout = layoutResult.layoutInput.text.text
    val node = onNodeWithText(allTextInLayout, useUnmergedTree = true)
    val bounds = node.fetchSemanticsNode().boundsInRoot
    val charPositionInLayout = layoutResult.getBoundingBox(charIndex)
    return Offset(bounds.left + charPositionInLayout.left, bounds.top)
}

fun ComposeTestRule.movePointerOverComment(comment: String, layoutResult: TextLayoutResult) {
    val textInLayout = layoutResult.layoutInput.text.text
    movePointerOverCharacter(textInLayout.indexOf(comment), layoutResult)
}

fun ComposeTestRule.movePointerToTheRightOfTheComment(comment: String, layoutResult: TextLayoutResult) {
    val textInLayout = layoutResult.layoutInput.text.text
    val charIndexAtTheEndOfTheComment = textInLayout.indexOf(comment) + comment.length - 1
    movePointerToTheRightOfTheCharacter(charIndexAtTheEndOfTheComment, layoutResult)
}

fun ComposeTestRule.movePointerBelowTheText(layoutResult: TextLayoutResult) {
    val lineBottom = layoutResult.getLineBottom(0)
    performMouseInput {
        moveTo(Offset(0f, lineBottom + 10f))
    }
}

/**
 * Move the pointer over the comment given by the named comment attribute, which
 * is what shows that comment's tooltip and highlights its row.
 */
fun ComposeTestRule.movePointerOverCommentRow(name: String, idPrefix: String = "") {
    onNode(hasContentDescriptionStartingWith("$idPrefix$COMMENT_TEXT_PREFIX$name"), useUnmergedTree = true)
        .performMouseInput { moveTo(center) }
    waitForIdle()
}

/**
 * Move the pointer over the first characters of the comment given by the named
 * comment attribute, rather than over the middle of it.
 */
fun ComposeTestRule.movePointerOverStartOfComment(name: String, idPrefix: String = "") {
    onNode(hasContentDescriptionStartingWith("$idPrefix$COMMENT_TEXT_PREFIX$name"), useUnmergedTree = true)
        .performMouseInput { moveTo(Offset(2f, center.y)) }
    waitForIdle()
}

/**
 * Move the pointer over the half of a pending replacement row that shows the
 * comment coming in, as against the one going out.
 */
fun ComposeTestRule.movePointerOverReplacementHalf(name: String, idPrefix: String = "") {
    onNode(
        hasContentDescriptionStartingWith("$idPrefix$COMMENT_REPLACEMENT_TEXT_PREFIX$name"),
        useUnmergedTree = true
    ).performMouseInput { moveTo(center) }
    waitForIdle()
}

/**
 * Move the pointer over the text cell of the row previewing the given kind of
 * pending change, whose id carries that kind rather than the plain prefix.
 */
fun ComposeTestRule.movePointerOverPendingCommentRow(pendingPrefix: String, name: String) {
    onNode(hasContentDescriptionStartingWith("$pendingPrefix$name"), useUnmergedTree = true)
        .performMouseInput { moveTo(center) }
    waitForIdle()
}

/**
 * Move the pointer away from every comment, to where no row can be under it.
 */
fun ComposeTestRule.movePointerAwayFromTheComments() {
    performMouseInput {
        moveTo(Offset(0f, 0f))
    }
    waitForIdle()
}

fun ComposeTestRule.requireConditionsToBeShowing(conditions: List<String>) {
    conditions.forEach { condition ->
        waitUntilAsserted {
            withClue("Condition $condition is not displayed") {
                onNodeWithContentDescription("$CONDITION_PREFIX$condition").assertIsDisplayed()
            }
        }
    }
}

fun ComposeTestRule.requireNoConditionsToBeShowing() {
    onAllNodesWithContentDescription(label = CONDITION_PREFIX, substring = true).assertCountEquals(0)
}
