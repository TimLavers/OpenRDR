package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.kb.*

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.assertRuleDescriptionIs(expected: String) {
    waitUntilExactlyOneExists(hasText(expected))
    onNodeWithTag(LAST_RULE_DESCRIPTION_DESCRIPTION).assertTextEquals(expected)
}
fun ComposeTestRule.cancelShowLastRule() {
    onNodeWithContentDescription(CLOSE_SHOW_LAST_RULE_DESCRIPTION).performClick()
}
fun ComposeTestRule.clickUndoLastRule() {
    onNodeWithContentDescription(UNDO_LAST_RULE_BUTTON_DESCRIPTION).performClick()
}
fun ComposeTestRule.clickUndoLastRuleConfirmationYesButton() {
    onNodeWithContentDescription(CONFIRM_UNDO_LAST_RULE_YES_OPTION_DESCRIPTION).performClick()
}
fun ComposeTestRule.clickUndoLastRuleConfirmationNoButton() {
    onNodeWithContentDescription(CONFIRM_UNDO_LAST_RULE_NO_OPTION_DESCRIPTION).performClick()
}
fun ComposeTestRule.assertUndoLastRuleButtonIsNotShowing() = onAllNodesWithContentDescription(UNDO_LAST_RULE_BUTTON_DESCRIPTION).assertCountEquals(0)
fun ComposeTestRule.assertUndoLastRuleButtonIsShowing() = onAllNodesWithContentDescription(UNDO_LAST_RULE_BUTTON_DESCRIPTION).assertCountEquals(1)
