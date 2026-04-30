package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_DESCRIPTION
import io.rippledown.mocks.DummyRowScope
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AttributesHeaderCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.attributeColumnWeight }.returns(0.5F)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.AttributesHeaderCell(  columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasContentDescription(ATTRIBUTE_HEADER_CELL_DESCRIPTION))
            onNodeWithContentDescription(ATTRIBUTE_HEADER_CELL_DESCRIPTION)
                .assertExists()
        }
    }
}