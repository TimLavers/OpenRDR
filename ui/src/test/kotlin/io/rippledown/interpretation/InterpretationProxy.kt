package io.rippledown.interpretation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.interpretation.*

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

fun ComposeTestRule.selectDifferencesTab() {
    onNodeWithContentDescription(INTERPRETATION_TAB_CHANGES).performClick()
}

fun ComposeTestRule.requireNoDifferencesTab() {
    onNodeWithContentDescription(INTERPRETATION_TAB_CHANGES).assertDoesNotExist()
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

fun ComposeTestRule.requireDifferencesTabToBeNotShowing() {
    onNodeWithContentDescription(INTERPRETATION_PANEL_CHANGES).assertDoesNotExist()
}

fun ComposeTestRule.requireDifferencesTabToBeShowing() {
    onNodeWithContentDescription(INTERPRETATION_PANEL_CHANGES).assertExists()
}

fun ComposeTestRule.requireInterpretationActionsDropdownMenu() {
    onNodeWithContentDescription(CHANGE_INTERPRETATION_BUTTON).assertIsDisplayed()
}

fun ComposeTestRule.clickChangeInterpretationButton() =
    onNodeWithContentDescription(CHANGE_INTERPRETATION_BUTTON).performClick()

fun ComposeTestRule.clickAddCommentMenu() = onNodeWithContentDescription(ADD_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

fun ComposeTestRule.clickReplaceCommentMenu() = onNodeWithContentDescription(REPLACE_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

fun ComposeTestRule.clickRemoveCommentMenu() = onNodeWithContentDescription(REMOVE_COMMENT_MENU)
    .assertIsDisplayed()
    .performClick()

fun ComposeTestRule.addNewComment(comment: String) {
    onNodeWithContentDescription(NEW_COMMENT_TEXT_FIELD)
        .assertIsDisplayed()
        .performTextInput(comment)
    waitForIdle()
}

fun ComposeTestRule.clickOKToAddNewComment() {
    onNodeWithContentDescription(OK_BUTTON)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickCancelAddNewComment() {
    onNodeWithContentDescription(CANCEL_BUTTON)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}





