@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.main.*

fun ComposeTestRule.enterKbName(text: String) {
    onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION).performTextInput(text)
}

fun ComposeTestRule.enterZipFileName(path: String) {
    onNodeWithContentDescription(IMPORT_KB_NAME_FIELD_DESCRIPTION).performTextInput(path)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireEnteredKBName(text: String) {
    waitUntilAtLeastOneExists(hasText(text))
}

fun ComposeTestRule.assertOkButtonIsNotEnabled() = onNodeWithContentDescription(CREATE_KB_OK_BUTTON_DESCRIPTION).assertIsNotEnabled()
fun ComposeTestRule.assertImportButtonIsNotEnabled() = onNodeWithContentDescription(IMPORT_KB_OK_BUTTON_DESCRIPTION).assertIsNotEnabled()

fun ComposeTestRule.clickCreateButton() = onNodeWithContentDescription(CREATE_KB_OK_BUTTON_DESCRIPTION).performClick()
fun ComposeTestRule.clickImportButton() = onNodeWithText(IMPORT_KB_TEXT).performClick()
