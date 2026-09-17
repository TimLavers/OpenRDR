package io.rippledown.appbar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import io.rippledown.constants.kb.KB_NAME_ID

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.assertKbNameIs(expected: String) {
    waitUntilExactlyOneExists(hasText(expected))
    onNodeWithTag(KB_NAME_ID, useUnmergedTree = true).assertTextEquals(expected)
}
