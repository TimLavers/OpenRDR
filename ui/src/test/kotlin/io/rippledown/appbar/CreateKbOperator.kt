@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.main.*


fun ComposeTestRule.waitToVanish() {
    waitUntil {
        onAllNodesWithTag(CREATE_KB_NAME_FIELD_ID).fetchSemanticsNodes().isEmpty()
    }
}

fun ComposeTestRule.enterText(text: String) {
    onNodeWithTag(TEXT_INPUT_FIELD_TEST_TAG).performTextInput(text)
}

fun ComposeTestRule.enterZipFileName(path: String) {
    onNodeWithTag(TEXT_INPUT_FIELD_TEST_TAG).performTextInput(path)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireEnteredKBName(text: String) {
    waitUntilAtLeastOneExists(hasText(text))
}

fun ComposeTestRule.performTextClearance() = onNodeWithTag(TEXT_INPUT_FIELD_TEST_TAG).performTextClearance()

fun ComposeTestRule.assertCreateButtonIsEnabled() = onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsEnabled()

fun ComposeTestRule.assertOkButtonIsNotEnabled() = onNodeWithTag(TEXT_INPUT_OK_BUTTON_TEST_TAG).assertIsNotEnabled()
fun ComposeTestRule.assertImportButtonIsNotEnabled() = onNodeWithTag(TEXT_INPUT_OK_BUTTON_TEST_TAG).assertIsNotEnabled()

fun ComposeTestRule.clickCreateButton() = onNodeWithTag(TEXT_INPUT_OK_BUTTON_TEST_TAG).performClick()
fun ComposeTestRule.clickImportButton() = onNodeWithTag(TEXT_INPUT_OK_BUTTON_TEST_TAG).performClick()

fun ComposeTestRule.clickCancelButton() = onNodeWithTag(TEXT_INPUT_CANCEL_BUTTON_TEST_TAG).performClick()
