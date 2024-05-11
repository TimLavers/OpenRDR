@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.IMPORT_KB_TEXT

fun ComposeTestRule.assertKbNameIs(expected: String) {
    waitUntilExactlyOneExists(hasText(expected))
    onNodeWithTag(KB_SELECTOR_ID).assertTextEquals(expected)
}

fun ComposeTestRule.clickDropdown() = onNodeWithContentDescription(KB_CONTROL_DROPDOWN_DESCRIPTION).performClick()

fun ComposeTestRule.assertCreateKbMenuItemIsNotShowing() = onAllNodesWithText(CREATE_KB_TEXT).assertCountEquals(0)
fun ComposeTestRule.assertImportKbMenuItemIsNotShowing() = onAllNodesWithText(IMPORT_KB_TEXT).assertCountEquals(0)

fun ComposeTestRule.assertCreateKbMenuItemIsShowing() {
    onNodeWithText(CREATE_KB_TEXT).assertIsEnabled()
}
fun ComposeTestRule.assertImportKbMenuItemIsShowing() {
    onNodeWithText(IMPORT_KB_TEXT).assertIsEnabled()
}

fun ComposeTestRule.assertDropdownItemsContain(vararg items: String) {
    items.forEach { kbName ->
        onNodeWithContentDescription("$KB_INFO_ITEM$kbName").assertTextEquals(kbName)
    }
}

fun ComposeTestRule.clickCreateKbMenuItem() = onNodeWithContentDescription(CREATE_KB_TEXT).performClick()
fun ComposeTestRule.clickImportKbMenuItem() = onNodeWithContentDescription(IMPORT_KB_TEXT).performClick()
