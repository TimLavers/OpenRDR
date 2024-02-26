package io.rippledown.caseview

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_TEXT
import io.rippledown.mocks.DummyLazyItemScope
import io.rippledown.model.lastWeek
import io.rippledown.model.today
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
        every { columnWidths.referenceRangeColumnWeight }.returns(0.2F)
        val lazyItemScope: LazyItemScope = DummyLazyItemScope()
        composeTestRule.setContent {
            lazyItemScope.HeaderRow( columnWidths, dates)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(ATTRIBUTE_HEADER_CELL_TEXT))
            waitUntilExactlyOneExists(hasText(formatDate(lastWeek)))
            waitUntilExactlyOneExists(hasText(formatDate(today)))
            waitUntilExactlyOneExists(hasText(""))
        }
    }
}