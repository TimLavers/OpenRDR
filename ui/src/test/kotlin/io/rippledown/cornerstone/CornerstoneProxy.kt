package io.rippledown.cornerstone

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.PREVIOUS_BUTTON

fun ComposeTestRule.clickNext() = onNodeWithContentDescription(NEXT_BUTTON).performClick()
fun ComposeTestRule.clickPrevious() = onNodeWithContentDescription(PREVIOUS_BUTTON).performClick()
