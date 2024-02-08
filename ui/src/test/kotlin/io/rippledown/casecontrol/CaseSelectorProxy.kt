package io.rippledown.casecontrol

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.CASE_NAME_PREFIX

fun ComposeTestRule.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    caseNames.forEach {  caseName ->
        onNode(hasTestTag("$CASE_NAME_PREFIX$caseName")).assertExists()
    }
}

fun ComposeTestRule.selectCaseByName(caseName: String) {
    onNode(hasTestTag("$CASE_NAME_PREFIX$caseName")).performClick()
}
fun ComposeTestRule.selectCaseByNameUsingContentDescription(caseName: String) {
    onNode(hasContentDescriptionExactly("$CASE_NAME_PREFIX$caseName"), true).performClick()
}

fun ComposeTestRule.requireNumberOfCasesOnCaseList(expected: Int) {
    onNodeWithTag(CASELIST_ID).onChildren().assertCountEquals(expected)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireNumberOfCases(expected: Int) {
    waitUntilExactlyOneExists(hasText("$CASES $expected"), timeoutMillis = 2_000)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireCaseSelectorNotToBeShowing() {
    waitUntilDoesNotExist(hasTestTag(CASELIST_ID), timeoutMillis = 2_000)
}
