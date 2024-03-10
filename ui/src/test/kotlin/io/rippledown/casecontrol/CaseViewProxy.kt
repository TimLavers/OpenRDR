package io.rippledown.casecontrol

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID


@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForCaseToBeShowing(caseName: String) {
    waitUntilAtLeastOneExists(hasTestTag("$CASEVIEW_CASE_NAME_ID$caseName"))
}

