package io.rippledown.cornerstone

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID

fun ComposeTestRule.requireCornerstoneCase(name: String) {
    onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
        .assertTextEquals(name)
}
fun ComposeTestRule.requireNoCornerstoneCaseToBeShowing() {
    onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
        .assertDoesNotExist()
}
