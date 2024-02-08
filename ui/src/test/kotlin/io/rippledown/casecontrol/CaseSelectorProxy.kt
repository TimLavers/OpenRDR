@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.casecontrol

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.CASE_NAME_PREFIX

fun ComposeTestRule.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    waitForNumberOfCases(caseNames.size)
    caseNames.forEach {  caseName ->
        onNode(hasTestTag("$CASE_NAME_PREFIX$caseName")).assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.selectCaseByName(caseName: String) {
    val matcher = hasTestTag("$CASE_NAME_PREFIX$caseName")
    waitUntilExactlyOneExists(matcher, timeoutMillis = 2_000)
    onNode(matcher).performClick()
}
fun ComposeTestRule.selectCaseByNameUsingContentDescription(caseName: String) {
    onNode(hasContentDescriptionExactly("$CASE_NAME_PREFIX$caseName"), true).performClick()
}

fun ComposeTestRule.requireNumberOfCasesOnCaseList(expected: Int) {
    onNodeWithTag(CASELIST_ID).onChildren().assertCountEquals(expected)
}

fun ComposeTestRule.waitForNumberOfCases(expected: Int) {
    waitUntilExactlyOneExists(hasText("$CASES $expected"), timeoutMillis = 2_000)
}

fun ComposeTestRule.waitForCaseSelectorNotToBeShowing() {
    waitUntilDoesNotExist(hasTestTag(CASELIST_ID), timeoutMillis = 2_000)
}
