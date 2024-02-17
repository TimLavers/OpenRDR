package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_DESCRIPTION
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_TEXT
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
            waitUntilExactlyOneExists(hasText(ATTRIBUTE_HEADER_CELL_TEXT))
            val sn = onNodeWithText(ATTRIBUTE_HEADER_CELL_TEXT).fetchSemanticsNode()
            println(sn)
            onNodeWithText(ATTRIBUTE_HEADER_CELL_TEXT)
                .assertIsDisplayed()
                .assertContentDescriptionEquals(ATTRIBUTE_HEADER_CELL_DESCRIPTION)
        }
    }
}