package io.rippledown.caseview

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_DESCRIPTION
import io.rippledown.constants.caseview.REFERENCE_RANGE_HEADER_CELL_DESCRIPTION
import io.rippledown.constants.caseview.UNITS_HEADER_CELL_DESCRIPTION
import io.rippledown.mocks.DummyLazyItemScope
import io.rippledown.utils.lastWeek
import io.rippledown.utils.today
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HeaderRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val dates = listOf( lastWeek, today)
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.attributeColumnWeight }.returns(0.2F)
        every { columnWidths.valueColumnWeight() }.returns(0.3F)
        every { columnWidths.valueRangeGapWeight }.returns(0.1F)
        every { columnWidths.referenceRangeColumnWeight }.returns(0.2F)
        every { columnWidths.unitsColumnWeight }.returns(0.1F)
        val lazyItemScope: LazyItemScope = DummyLazyItemScope()
        composeTestRule.setContent {
            HeaderRow( columnWidths, dates)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasContentDescription(ATTRIBUTE_HEADER_CELL_DESCRIPTION))
            waitUntilExactlyOneExists(hasText(formatDate(lastWeek)))
            waitUntilExactlyOneExists(hasText(formatDate(today)))
            waitUntilExactlyOneExists(hasContentDescription(REFERENCE_RANGE_HEADER_CELL_DESCRIPTION))
            waitUntilExactlyOneExists(hasContentDescription(UNITS_HEADER_CELL_DESCRIPTION))
        }
    }
}