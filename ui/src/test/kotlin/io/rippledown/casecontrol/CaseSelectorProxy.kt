package io.rippledown.casecontrol

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX

fun ComposeTestRule.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    caseNames.forEach {  caseName ->
        onNode(hasTestTag("$CASE_NAME_PREFIX$caseName")).assertExists()
    }
}

fun ComposeTestRule.selectCaseByName(caseName: String) {
    onNode(hasTestTag("$CASE_NAME_PREFIX$caseName")).performClick()
}

fun ComposeTestRule.requireNumberOfCasesOnCaseList(expected: Int) {
    onNodeWithTag(CASELIST_ID).onChildren().assertCountEquals(expected)
}