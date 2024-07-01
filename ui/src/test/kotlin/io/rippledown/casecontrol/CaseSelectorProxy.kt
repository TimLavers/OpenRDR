@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.casecontrol

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX

fun ComposeTestRule.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    caseNames.forEach { caseName ->
        onNode(caseMatcher(caseName)).assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)

fun ComposeTestRule.selectCaseByName(caseName: String) {
    waitUntilExactlyOneExists(caseMatcher(caseName))
    onNode(caseMatcher(caseName))
        .assertIsDisplayed()
        .performClick()
    waitForIdle()
}

fun ComposeTestRule.requireCaseToBeFocused(caseName: String) {
    onNode(caseMatcher(caseName))
    onNode(caseMatcher(caseName)).assertIsFocused()
}

fun caseMatcher(caseName: String) = hasContentDescriptionExactly("$CASE_NAME_PREFIX$caseName")

fun ComposeTestRule.requireNumberOfCasesOnCaseList(expected: Int) {
    onNodeWithContentDescription(CASELIST_ID).onChildren().assertCountEquals(expected)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForNumberOfCases(expected: Int) {
    waitUntilExactlyOneExists(hasText(expected.toString()), timeoutMillis = 2_000)
}

fun ComposeTestRule.requireCaseSelectorToBeDisplayed() {
    onNodeWithContentDescription(CASELIST_ID).assertIsDisplayed()
}

fun ComposeTestRule.requireCaseSelectorNotToBeDisplayed() {
    onNodeWithContentDescription(CASELIST_ID).assertDoesNotExist()
}

fun ComposeTestRule.downArrowOnCase(caseName: String) {
    onNode(caseMatcher(caseName), true).performKeyInput {
        keyDown(Key.Companion.DirectionDown)
    }
    waitForIdle()
}

fun ComposeTestRule.upArrowOnCase(caseName: String) {
    onNode(caseMatcher(caseName), true).performKeyInput {
        keyDown(Key.Companion.DirectionUp)
    }
    waitForIdle()
}
