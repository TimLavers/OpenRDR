package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.assertKbNameIs(expected: String) {
    waitUntilExactlyOneExists(hasText(expected))
    onNodeWithTag(KB_NAME_ID, useUnmergedTree = true).assertTextEquals(expected)
}

fun ComposeTestRule.clickDropdown() = onNodeWithContentDescription(KB_CONTROL_DROPDOWN_DESCRIPTION).performClick()
