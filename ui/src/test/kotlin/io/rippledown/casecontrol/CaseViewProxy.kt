package io.rippledown.casecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASE_NAME_PREFIX


fun ComposeTestRule.requireCaseToBeShowing(caseName: String) {
    onNode(hasTestTag("$CASE_NAME_PREFIX$caseName")).assertIsDisplayed()
}
