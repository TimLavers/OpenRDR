@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.interpretation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.*
import io.rippledown.decoration.BACKGROUND_COLOR
import io.rippledown.main.LEFT_INFO_MESSAGE_ID
import io.rippledown.main.RIGHT_INFO_MESSAGE_ID
import io.rippledown.rule.waitUntilAsserted
import io.rippledown.utils.dump
import org.assertj.swing.edt.GuiActionRunner


@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireInterpretation(text: String) {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD, useUnmergedTree = true).assertTextEquals(text)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireInterpretationForCornerstone(text: String) {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE, useUnmergedTree = true).assertTextEquals(
        text
    )
}

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
    GuiActionRunner.execute {
        waitUntil { onNodeWithContentDescription("$prefix$comment").isDisplayed() }
        onNodeWithContentDescription("$prefix$comment").performClick()
        waitForIdle()

    }
}

fun ComposeTestRule.clickOKToAddNewComment() {
    waitForIdle()
    waitTillButtonIsEnabled(OK_BUTTON_FOR_ADD_COMMENT)
    onNodeWithContentDescription(OK_BUTTON_FOR_ADD_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickOKToReplaceComment() {
    waitForIdle()
    waitTillButtonIsEnabled(OK_BUTTON_FOR_REPLACE_COMMENT)
    waitForIdle()
    onNodeWithContentDescription(OK_BUTTON_FOR_REPLACE_COMMENT)
        .assertIsEnabled()
        .performClick()
    waitForIdle()
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
    GuiActionRunner.execute {
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
    val annotatedString = layoutResult.layoutInput.text
    annotatedString.spanStyles.size shouldBe 0
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

fun ComposeTestRule.requireLeftInformationMessage(message: String) {
    waitUntil { onNodeWithContentDescription(LEFT_INFO_MESSAGE_ID).isDisplayed() }
    onNodeWithContentDescription(LEFT_INFO_MESSAGE_ID)
        .assertIsDisplayed()
        .assertTextEquals(message)
}

fun ComposeTestRule.requireRightInformationMessage(message: String) {
    onNodeWithContentDescription(RIGHT_INFO_MESSAGE_ID)
        .assertIsDisplayed()
        .assertTextEquals(message)
}

private fun ComposeTestRule.performMouseInput(action: MouseInjectionScope.() -> Unit) {
    onAllNodes(isRoot())[0].performMouseInput { action() }
}

fun ComposeTestRule.movePointerOverCharacter(charIndex: Int, layoutResult: TextLayoutResult) {
    val charPositionAbsolute = absoluteCharacterPosition(layoutResult, charIndex)
    performMouseInput {
        moveTo(charPositionAbsolute)
    }
}

fun ComposeTestRule.movePointerToTheRightOfTheCharacter(charIndex: Int, layoutResult: TextLayoutResult) {
    val charPositionAbsolute = absoluteCharacterPosition(layoutResult, charIndex)
    performMouseInput {
        moveTo(charPositionAbsolute + Offset(10f, 0f))
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
    val charPositionAbsolute = Offset(bounds.left + charPositionInLayout.left, bounds.top)
    return charPositionAbsolute
}

fun ComposeTestRule.movePointerOverComment(comment: String, layoutResult: TextLayoutResult) {
    val textInLayout = layoutResult.layoutInput.text.text
    val charIndex = textInLayout.indexOf(comment)
    movePointerOverCharacter(charIndex, layoutResult)
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
