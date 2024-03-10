@file:OptIn(ExperimentalTestApi::class, ExperimentalTestApi::class, ExperimentalTestApi::class)

package io.rippledown.casecontrol

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.CASE_NAME_PREFIX

fun ComposeTestRule.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    caseNames.forEach {  caseName ->
        onNode(caseMatcher(caseName)).assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)

fun ComposeTestRule.selectCaseByName(caseName: String) {
    waitUntilExactlyOneExists(caseMatcher(caseName))
    onNode(caseMatcher(caseName)).performClick()
}

fun ComposeTestRule.selectCaseByNameUsingContentDescription(caseName: String) {
    onNode(caseMatcher(caseName), true).performClick()
}
 fun caseMatcher(caseName: String) = hasContentDescriptionExactly("$CASE_NAME_PREFIX$caseName")

fun ComposeTestRule.requireNumberOfCasesOnCaseList(expected: Int) {
    onNodeWithTag(CASELIST_ID).onChildren().assertCountEquals(expected)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForNumberOfCases(expected: Int) {
    waitUntilExactlyOneExists(hasText("$CASES $expected"), timeoutMillis = 2_000)
}

fun ComposeTestRule.waitForCaseSelectorNotToBeShowing() {
    waitUntilDoesNotExist(hasTestTag(CASELIST_ID), timeoutMillis = 2_000)
}