package io.rippledown.interpretation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick

fun ComposeTestRule.requireNodeText(level: Int, parentIndex: Int, index: Int, expected: String) {
    onNodeWithContentDescription(textContentDescription(level, parentIndex, index, expected)).assertTextEquals(expected)
}

fun ComposeTestRule.requireNodeTextNotShowing(level: Int, parentIndex: Int, index: Int, expected: String) {
    onNodeWithContentDescription(textContentDescription(level, parentIndex, index, expected)).assertDoesNotExist()
}

fun ComposeTestRule.clickNode(level: Int, parentIndex: Int, index: Int, expected: String) {
    onNodeWithContentDescription(iconContentDescription(level, parentIndex, index, expected)).performClick()
}