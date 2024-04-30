@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.diffview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.interpretation.*

fun ComposeTestRule.requireNumberOfDiffRows(expected: Int) {
    onNodeWithContentDescription(DIFF_VIEW).onChildren().assertCountEquals(expected)
}

fun ComposeTestRule.requireBuildIconForRow(rowIndex: Int) {
    onNodeWithContentDescription("$ICON_PREFIX$rowIndex").assertIsDisplayed()
}

fun ComposeTestRule.requireNoBuildIconForRow(rowIndex: Int) {
    onNodeWithContentDescription("$ICON_PREFIX$rowIndex").assertDoesNotExist()
}

fun ComposeTestRule.clickBuildIconForRow(rowIndex: Int) {
    moveMouseOverRow(rowIndex) // to make the icon visible
    onNodeWithContentDescription("$ICON_PREFIX$rowIndex").performClick()
    waitForIdle()
}

fun ComposeTestRule.moveMouseOverRow(rowIndex: Int) {
    onNodeWithContentDescription("$DIFF_ROW_PREFIX$rowIndex")
        .performMouseInput { moveTo(Offset.Zero) }
}

fun ComposeTestRule.requireOriginalTextInRow(rowIndex: Int, expected: String) {
    onNodeWithContentDescription("$ORIGINAL_PREFIX$rowIndex").assertTextEquals(expected)
}

fun ComposeTestRule.requireChangedTextInRow(rowIndex: Int, expected: String) {
    onNodeWithContentDescription("$CHANGED_PREFIX$rowIndex").assertTextEquals(expected)
}