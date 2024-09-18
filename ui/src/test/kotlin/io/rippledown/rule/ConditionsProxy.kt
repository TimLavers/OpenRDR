package io.rippledown.rule

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.main.EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_FIELD_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_OK_BUTTON_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_TEXT_1_DESCRIPTION
import io.rippledown.constants.rule.*

fun ComposeTestRule.requireRuleMakerToBeDisplayed() {
    val onNodeWithContentDescription = onNodeWithContentDescription(RULE_MAKER)
    onNodeWithContentDescription.assertIsDisplayed()
}

fun ComposeTestRule.requireRuleMakerNotToBeDisplayed() {
    onNodeWithContentDescription(RULE_MAKER).assertDoesNotExist()
}

fun ComposeTestRule.requireAvailableConditionsToBeDisplayed(conditions: List<String>) {
    onNodeWithContentDescription(AVAILABLE_CONDITIONS).onChildren().assertCountEquals(conditions.size)
    conditions.forEachIndexed { index, condition ->
        onNodeWithContentDescription("$AVAILABLE_CONDITION_PREFIX$index")
            .assertTextEquals(condition)
            .assertIsDisplayed()
    }
}

fun ComposeTestRule.requireNoAvailableConditionsToBeDisplayed() {
    onNodeWithContentDescription(AVAILABLE_CONDITIONS).onChildren().assertCountEquals(0)
}

fun ComposeTestRule.requireSelectedConditionsToBeDisplayed(conditions: List<String>) {
    onNodeWithContentDescription(SELECTED_CONDITIONS).onChildren().assertCountEquals(conditions.size)
    conditions.forEachIndexed { index, condition ->
        onNodeWithContentDescription("$SELECTED_CONDITION_PREFIX$index")
            .assertTextEquals(condition)
            .assertIsDisplayed()
    }
}

fun ComposeTestRule.clickAvailableCondition(index: Int) {
    onNodeWithContentDescription("$AVAILABLE_CONDITION_PREFIX$index")
        .assertIsDisplayed()
        .performClick()
}

fun ComposeTestRule.clickAvailableConditionWithText(text: String) {
    onNodeWithContentDescription(AVAILABLE_CONDITIONS)
        .onChildren().filterToOne(hasText(text))
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.clickAvailableConditions(conditions: List<String>) {
    conditions.forEach { condition ->
        clickAvailableConditionWithText(condition)
    }
}

fun ComposeTestRule.clickSelectedConditionWithText(text: String) {
    onNodeWithContentDescription(SELECTED_CONDITIONS)
        .onChildren().filterToOne(hasText(text)).performClick()
    waitForIdle()
}

fun ComposeTestRule.clickSelectedConditions(conditions: List<String>) {
    conditions.forEach { condition ->
        clickSelectedConditionWithText(condition)
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.hoverOverSelectedCondition(index: Int) {
    onNodeWithContentDescription("$SELECTED_CONDITION_PREFIX$index").performMouseInput {
        moveTo(Offset.Zero)
    }
}

fun ComposeTestRule.removeSelectedCondition(index: Int) {
    onNodeWithContentDescription("$REMOVE_CONDITION_ICON_PREFIX$index")
        .assertIsDisplayed()
        .performClick()
}

fun ComposeTestRule.clickFinishRuleButton() {
    onNodeWithContentDescription(FINISH_RULE_BUTTON)
        .assertExists()
        .performClick()
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.clickCancelRuleButton() {
    waitUntil { onNodeWithContentDescription(CANCEL_RULE_BUTTON).isDisplayed() }
    onNodeWithContentDescription(CANCEL_RULE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.enterNewVariableValueInConditionEditor(text: String) {
    onNodeWithContentDescription(EDIT_CONDITION_FIELD_DESCRIPTION).performTextClearance()
    onNodeWithContentDescription(EDIT_CONDITION_FIELD_DESCRIPTION).performTextInput(text)
}

fun ComposeTestRule.clickConditionEditorOkButton() {
    onNodeWithContentDescription(EDIT_CONDITION_OK_BUTTON_DESCRIPTION)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}
fun ComposeTestRule.clickConditionEditorCancelButton() {
    onNodeWithContentDescription(EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION)
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.requireConditionConstantTextFirstPartToBe(expected: String) {
    onNodeWithContentDescription(EDIT_CONDITION_TEXT_1_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals(expected)
}
