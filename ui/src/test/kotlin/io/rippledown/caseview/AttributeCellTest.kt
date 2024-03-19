package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.mocks.DummyRowScope
import io.rippledown.model.Attribute
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AttributeCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val tsh = Attribute(12, "TSH")
    private val columnWidths = ColumnWidths(7)

    @Test
    fun `show result that does not have units`() {
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.AttributeCell(3, "Bondi", tsh, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(tsh.name))
        }
    }
}

