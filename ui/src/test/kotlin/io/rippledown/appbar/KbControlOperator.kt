@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.CREATE_KB_ITEM_ID
import io.rippledown.constants.main.CREATE_KB_TEXT

fun ComposeTestRule.assertKbNameIs(expected: String) {
    waitUntilExactlyOneExists(hasText(expected))
    onNodeWithTag(KB_SELECTOR_ID).assertTextEquals(expected)
}

fun ComposeTestRule.clickDropdown() = onNodeWithContentDescription(KB_CONTROL_DROPDOWN_DESCRIPTION).performClick()

fun ComposeTestRule.assertCreateKbButtonIsNotShowing() = onAllNodesWithText(CREATE_KB_TEXT).assertCountEquals(0)

fun ComposeTestRule.assertCreateKbButtonIsShowing() {
    onNodeWithText(CREATE_KB_TEXT).assertIsEnabled()
}

fun ComposeTestRule.assertDropdownItemsContain(vararg items: String) {
    items.forEach { kbName ->
        onNodeWithContentDescription("$KB_INFO_ITEM$kbName").assertTextEquals(kbName)
    }
}

fun ComposeTestRule.clickCreateKbButton() {
    onNodeWithTag(CREATE_KB_ITEM_ID).performClick()
}
