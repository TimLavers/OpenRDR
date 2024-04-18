@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.main.CREATE_KB_CANCEL_BUTTON_ID
import io.rippledown.constants.main.CREATE_KB_NAME_FIELD_ID
import io.rippledown.constants.main.CREATE_KB_OK_BUTTON_ID


fun ComposeTestRule.waitToVanish() {
    waitUntil {
        onAllNodesWithTag(CREATE_KB_NAME_FIELD_ID).fetchSemanticsNodes().isEmpty()
    }
}

fun ComposeTestRule.enterKBName(text: String) {
    onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextInput(text)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireEnteredKBName(text: String) {
    waitUntilAtLeastOneExists(hasText(text))
    println(onNodeWithTag(CREATE_KB_NAME_FIELD_ID).printToString())
}

fun ComposeTestRule.performTextClearance() = onNodeWithTag(CREATE_KB_NAME_FIELD_ID).performTextClearance()

fun ComposeTestRule.assertCreateButtonIsEnabled() = onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsEnabled()

fun ComposeTestRule.assertOkButtonIsNotEnabled() = onNodeWithTag(CREATE_KB_OK_BUTTON_ID).assertIsNotEnabled()

fun ComposeTestRule.clickCreateButton() = onNodeWithTag(CREATE_KB_OK_BUTTON_ID).performClick()

fun ComposeTestRule.clickCancelButton() = onNodeWithTag(CREATE_KB_CANCEL_BUTTON_ID).performClick()
