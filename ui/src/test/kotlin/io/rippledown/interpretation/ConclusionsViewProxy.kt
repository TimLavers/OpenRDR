package io.rippledown.interpretation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick

fun ComposeTestRule.requireComment(commentIndex: Int, expected: String) {
    onNodeWithContentDescription(textContentDescription(1, 0, commentIndex, expected)).assertTextEquals(expected)
}

fun ComposeTestRule.clickComment(commentIndex: Int, expected: String) {
    onNodeWithContentDescription(iconContentDescription(1, 0, commentIndex, expected)).performClick()
}

fun ComposeTestRule.requireConditionForComment(commentIndex: Int, conditionIndex: Int, expected: String) {
    onNodeWithContentDescription(textContentDescription(2, commentIndex, conditionIndex, expected)).assertTextEquals(
        expected
    )
}

fun ComposeTestRule.requireConditionNotShowing(commentIndex: Int, conditionIndex: Int, expected: String) {
    onNodeWithContentDescription(textContentDescription(2, commentIndex, conditionIndex, expected)).assertDoesNotExist()
}

