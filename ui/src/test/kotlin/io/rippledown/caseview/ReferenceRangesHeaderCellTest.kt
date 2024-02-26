package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.caseview.REFERENCE_RANGE_HEADER_CELL_DESCRIPTION
import io.rippledown.mocks.DummyRowScope
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReferenceRangesHeaderCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.referenceRangeColumnWeight }.returns(0.5F)

        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ReferenceRangesHeaderCell( columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(""))
            onNodeWithText("")
                .assertContentDescriptionEquals(REFERENCE_RANGE_HEADER_CELL_DESCRIPTION)
        }
    }
}