package io.rippledown.cornerstone

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.EXEMPT_BUTTON
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.OF
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.rule.waitUntilAsserted

fun ComposeTestRule.clickNext() = onNodeWithContentDescription(NEXT_BUTTON).performClick()
fun ComposeTestRule.clickPrevious() = onNodeWithContentDescription(PREVIOUS_BUTTON).performClick()
fun ComposeTestRule.clickExempt() = onNodeWithContentDescription(EXEMPT_BUTTON).performClick()

fun ComposeTestRule.requireIndexAndTotalToBeDisplayed(index: Int, total: Int) {
    waitUntilAsserted(1_000) {
        onNodeWithContentDescription(INDEX_AND_TOTAL_ID)
            .assertTextEquals("${index + 1} $OF $total") // use 1-based index for display
    }
}
fun ComposeTestRule.requireCornerstoneCase(name: String) {
    onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
        .assertTextEquals(name)
}
fun ComposeTestRule.requireNoCornerstoneCaseToBeShowing() {
    onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
        .assertDoesNotExist()
}
