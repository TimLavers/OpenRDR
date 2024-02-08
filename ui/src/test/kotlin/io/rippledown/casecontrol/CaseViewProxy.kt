package io.rippledown.casecontrol

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASE_HEADING


@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForCaseToBeShowing(caseName: String) {
    waitUntilExactlyOneExists(hasText("$CASE_HEADING$caseName"), timeoutMillis = 2_000)
}

