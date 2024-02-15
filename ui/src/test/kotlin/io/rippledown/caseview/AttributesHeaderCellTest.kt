package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.mocks.DummyRowScope
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AttributesHeaderCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.AttributesHeaderCell(  0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("Attributes"))

        }
    }
}