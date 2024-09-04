@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.interpretation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.interpretation.*
import io.rippledown.utils.dump
import java.lang.Thread.sleep

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireInterpretation(text: String) {
    //need to set useUnmergedTree to true to avoid the issue of the text field being merged with the label
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD, useUnmergedTree = true).assertTextEquals(text)
}

fun ComposeTestRule.selectConclusionsTab() {
    onNodeWithContentDescription(INTERPRETATION_TAB_CONCLUSIONS).performClick()
}

fun ComposeTestRule.requireConclusionsPanelToBeShowing() {
    onNodeWithContentDescription(INTERPRETATION_PANEL_CONCLUSIONS).assertIsDisplayed()
}

fun ComposeTestRule.requireInterpretationActionsDropdownMenu() {
    onNodeWithContentDescription(CHANGE_INTERPRETATION_BUTTON).assertIsDisplayed()
}

fun ComposeTestRule.clickChangeInterpretationButton() =
    onNodeWithContentDescription(CHANGE_INTERPRETATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

fun ComposeTestRule.clickAddCommentMenu() = onNodeWithContentDescription(ADD_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()


fun ComposeTestRule.clickReplaceCommentMenu() = onNodeWithContentDescription(REPLACE_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

fun ComposeTestRule.clickRemoveCommentMenu() = onNodeWithContentDescription(REMOVE_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

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
    sleep(500) //TODO remove this sleep
    clickCommentToRemove(comment)
    sleep(500) //TODO remove this sleep
    clickOKToRemoveComment()
}

fun ComposeTestRule.clickCommentToRemove(comment: String) {
    clickComment(REMOVE_COMMENT_PREFIX, comment)
}

fun ComposeTestRule.clickCommentToBeReplaced(comment: String) {
    clickComment(REPLACED_COMMENT_PREFIX, comment)
}

fun ComposeTestRule.clickComment(prefix: String, comment: String) {
    waitUntil { onNodeWithContentDescription("$prefix$comment").isDisplayed() }
    onNodeWithContentDescription("$prefix$comment").performClick()
    waitForIdle()
}

fun ComposeTestRule.clickOKToAddNewComment() {
    onNodeWithContentDescription(OK_BUTTON_FOR_ADD_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickOKToReplaceComment() {
    waitTillButtonIsEnabled(OK_BUTTON_FOR_REPLACE_COMMENT)
    onNodeWithContentDescription(OK_BUTTON_FOR_REPLACE_COMMENT)
        .assertIsEnabled()
        .performClick()
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
    onNodeWithContentDescription(OK_BUTTON_FOR_REMOVE_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
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
    onNodeWithContentDescription(CANCEL_BUTTON_FOR_ADD_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickCancelReplaceComment() {
    onNodeWithContentDescription(CANCEL_BUTTON_FOR_REPLACE_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickCancelRemoveComment() {
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

fun ComposeTestRule.requireTextToBeHighlighted(text: String) {
    TODO("Not yet implemented")
}

fun ComposeTestRule.movePointerOverText(text: String) {
    val node = onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD)
    onRoot().dump()

    val bounds = node.fetchSemanticsNode().boundsInRoot
    val center = bounds.center
    println("bounds = ${bounds}, center = $center")

    onRoot().performMouseInput {
        moveTo(center)
    }
}

fun ComposeTestRule.movePointerOverWord(text: String, word: String) {
    onRoot().dump()
    val node = onNodeWithText(text, useUnmergedTree = true)
    val semanticsNode = node.fetchSemanticsNode()
    val styles = semanticsNode.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.spanStyles
    println("styles = $styles")

}







