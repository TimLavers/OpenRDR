package io.rippledown.interpretation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.interpretation.*
import java.lang.Thread.sleep

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireInterpretation(text: String) {
    //need to set useUnmergedTree to true to avoid the issue of the text field being merged with the label
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD, useUnmergedTree = true).assertTextEquals(text)
}

fun ComposeTestRule.enterInterpretation(enteredText: String) {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).performTextInput(enteredText)
}

fun ComposeTestRule.replaceInterpretationBy(enteredText: String) {
    onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).performTextReplacement(enteredText)
}

fun ComposeTestRule.selectConclusionsTab() {
    onNodeWithContentDescription(INTERPRETATION_TAB_CONCLUSIONS).performClick()
}

fun ComposeTestRule.requireConclusionsPanelToBeShowing() {
    onNodeWithContentDescription(INTERPRETATION_PANEL_CONCLUSIONS).assertIsDisplayed()
}

fun ComposeTestRule.requireBadgeOnDifferencesTabNotToBeShowing() {
    onNodeWithContentDescription(BADGE_CONTENT_DESCRIPTION).assertDoesNotExist()
}

fun ComposeTestRule.requireBadgeOnDifferencesTabToShow(expected: Int) {
    onNodeWithContentDescription(
        BADGE_CONTENT_DESCRIPTION,
        useUnmergedTree = true
    ).assertTextEquals(expected.toString())
}

fun ComposeTestRule.requireInterpretationActionsDropdownMenu() {
    onNodeWithContentDescription(CHANGE_INTERPRETATION_BUTTON).assertIsDisplayed()
}

fun ComposeTestRule.clickChangeInterpretationButton() =
    onNodeWithContentDescription(CHANGE_INTERPRETATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

fun ComposeTestRule.clickAddCommentMenu() {
    onNodeWithContentDescription(ADD_COMMENT_MENU)
        .assertIsDisplayed()
        .performClick()
}

fun ComposeTestRule.clickReplaceCommentMenu() = onNodeWithContentDescription(REPLACE_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

fun ComposeTestRule.clickRemoveCommentMenu() = onNodeWithContentDescription(REMOVE_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

private fun ComposeTestRule.enterCommentToBeAdded(comment: String) {
    onNodeWithContentDescription(NEW_COMMENT_TEXT_FIELD)
        .assertIsDisplayed()
        .performTextInput(comment)
}

private fun ComposeTestRule.enterCommentToBeTheReplacement(comment: String) {
    onNodeWithContentDescription(REPLACEMENT_COMMENT_TEXT_FIELD)
        .assertIsDisplayed()
        .performTextInput(comment)
}

fun ComposeTestRule.addNewComment(comment: String) {
    enterCommentToBeAdded(comment)
    sleep(1_000) //TODO remove this sleep
    clickOKToAddNewComment()
    sleep(1_000) //TODO remove this sleep
}

fun ComposeTestRule.replaceComment(toBeReplaced: String, replacement: String) {
    sleep(500) //TODO remove this sleep
    clickCommentToBeReplaced(toBeReplaced)
    sleep(500) //TODO remove this sleep
    enterCommentToBeTheReplacement(replacement)
    sleep(500) //TODO remove this sleep
    clickOKToReplaceComment()
    sleep(500) //TODO remove this sleep
}

fun ComposeTestRule.clickCommentToRemove(comment: String) {
    clickComment(REMOVE_COMMENT_SELECTOR_PREFIX, comment)
}

fun ComposeTestRule.clickCommentToBeReplaced(comment: String) {
    clickComment(REPLACE_COMMENT_SELECTOR_PREFIX, comment)
}

fun ComposeTestRule.clickComment(prefix: String, comment: String) {
    waitUntil(2_000) { onNodeWithContentDescription("$prefix$comment").isDisplayed() }
    onNodeWithContentDescription("$prefix$comment").performClick()
    waitForIdle()
    sleep(1_000) //TODO remove this sleep
}

fun ComposeTestRule.requireCommentSelectorOptionsToBeDisplayed(prefix: String, options: List<String>) {
    requireDropDownMenuToBeDisplayed()
    options.forEach { option ->
        onNodeWithContentDescription("$prefix$option")
            .assertIsDisplayed()
    }
}

fun ComposeTestRule.requireCommentSelectorOptionsNotToBeDisplayed(prefix: String, options: List<String>) {
    requireDropDownMenuToBeDisplayed()
    options.forEach { option ->
        onNodeWithContentDescription("$prefix$option")
            .assertDoesNotExist()
    }
}

fun ComposeTestRule.requireDropDownMenuToBeDisplayed() {
    onNodeWithContentDescription(DROP_DOWN_TEXT_FIELD)
        .assertIsDisplayed()
}

fun ComposeTestRule.requireDropDownMenuForRemoveCommentToBeDisplayed() {
    onNodeWithContentDescription("$REMOVE_COMMENT_SELECTOR_PREFIX$DROP_DOWN_TEXT_FIELD", useUnmergedTree = true)
        .assertIsDisplayed()
}

fun ComposeTestRule.clickCommentDropDownMenu() {
    onNodeWithContentDescription(DROP_DOWN_TEXT_FIELD)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
    sleep(2_000) //TODO remove this sleep
}

fun ComposeTestRule.clickOKToAddNewComment() {
    onNodeWithContentDescription(OK_BUTTON_FOR_ADD_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickOKToReplaceComment() {
    onNodeWithContentDescription(OK_BUTTON_FOR_REPLACE_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickOKToRemoveComment() {
    onNodeWithContentDescription(OK_BUTTON_FOR_REMOVE_COMMENT)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
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

fun ComposeTestRule.requireCommentSelectorWithSelectedLabel(expected: String) {
    onNodeWithContentDescription(COMMENT_SELECTOR_LABEL, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals(expected)
}

fun ComposeTestRule.requireCommentSelectorForPrefixWithSelectedComment(prefix: String, expected: String) {
    onNodeWithContentDescription("$prefix$expected", useUnmergedTree = true)
        .assertIsDisplayed()
}





