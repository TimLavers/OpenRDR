package io.rippledown.casecontrol

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_HEADING


@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForCaseToBeShowing(caseName: String) {
    waitUntilExactlyOneExists(hasTestTag("$CASEVIEW_CASE_NAME_ID$caseName"))
}

