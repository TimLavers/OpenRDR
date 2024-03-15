package io.rippledown.casecontrol

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.caseview.valueCellContentDescription
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID


@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForCaseToBeShowing(caseName: String) {
    waitUntilAtLeastOneExists(hasTestTag("$CASEVIEW_CASE_NAME_ID$caseName"))
}

fun ComposeTestRule.requireValueForAttribute(attributeName: String, value: String) {
    onNodeWithContentDescription(valueCellContentDescription(attributeName, 0))
        .assert(hasText(value))
}

