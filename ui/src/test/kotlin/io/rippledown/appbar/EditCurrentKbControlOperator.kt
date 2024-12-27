@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*

fun ComposeTestRule.clickEditKbDropdown() = onNodeWithContentDescription(EDIT_CURRENT_KB_CONTROL_DROPDOWN_DESCRIPTION).performClick()

fun ComposeTestRule.assertEditKbDescriptionMenuItemIsShowing() {
    onNodeWithText(EDIT_KB_DESCRIPTION_BUTTON_TEXT).assertIsEnabled()
}
fun ComposeTestRule.clickKbDescriptionMenuItem() = onNodeWithText(EDIT_KB_DESCRIPTION_BUTTON_TEXT).performClick()

fun ComposeTestRule.assertKbDescriptionMenuItemIsNotShowing() = onAllNodesWithText(EDIT_KB_DESCRIPTION_BUTTON_TEXT).assertCountEquals(0)

fun ComposeTestRule.assertKbDescriptionOkButtonIsEnabled() = onNodeWithContentDescription(
    EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION
).assertIsEnabled()

fun ComposeTestRule.enterKbDescription(description: String) {
    onNodeWithContentDescription(EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION).performTextClearance()
    onNodeWithContentDescription(EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION).performTextInput(description)
}
fun ComposeTestRule.clickConfirmDescriptionButton() = onNodeWithContentDescription(EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION).performClick()


