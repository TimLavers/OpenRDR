package io.rippledown.rule

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.rule.*
import io.rippledown.utils.print

fun ComposeTestRule.requireRuleMakerToBeDisplayed() {
    val onNodeWithContentDescription = onNodeWithContentDescription(RULE_MAKER)
    onNodeWithContentDescription.print()
    onNodeWithContentDescription.assertIsDisplayed()
}

fun ComposeTestRule.requireRuleMakerNotToBeDisplayed() {
    onNodeWithContentDescription(RULE_MAKER).assertDoesNotExist()
}

fun ComposeTestRule.requireAvailableConditionsToBeDisplayed(conditions: List<String>) {
    onNodeWithContentDescription(AVAILABLE_CONDITIONS).onChildren().assertCountEquals(conditions.size)
    conditions.forEachIndexed { index, condition ->
        onNodeWithContentDescription("$AVAILABLE_CONDITION_PREFIX$index").assertTextEquals(condition)
    }
}

fun ComposeTestRule.requireSelectedConditionsToBeDisplayed(conditions: List<String>) {
    onNodeWithContentDescription(SELECTED_CONDITIONS).onChildren().assertCountEquals(conditions.size)
    conditions.forEachIndexed { index, condition ->
        onNodeWithContentDescription("$SELECTED_CONDITION_PREFIX$index").assertTextEquals(condition)
    }
}

fun ComposeTestRule.clickAvailableCondition(index: Int) {
    onNodeWithContentDescription("$AVAILABLE_CONDITION_PREFIX$index").performClick()
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.hoverOverSelectedCondition(index: Int) {
    onNodeWithContentDescription("$SELECTED_CONDITION_PREFIX$index").performMouseInput {
        moveTo(Offset.Zero)
    }
}

fun ComposeTestRule.removeSelectedCondition(index: Int) {
    onNodeWithContentDescription("$REMOVE_CONDITION_ICON_PREFIX$index").performClick()
}

fun ComposeTestRule.clickFinishRuleButton() {
    onNodeWithContentDescription(FINISH_RULE_BUTTON).assertIsDisplayed()
    onNodeWithContentDescription(FINISH_RULE_BUTTON).performClick()
    waitForIdle()
}

fun ComposeTestRule.clickCancelRuleButton() {
    onNodeWithContentDescription(CANCEL_RULE_BUTTON).performClick()
    waitForIdle()
}