package io.rippledown.caseview

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class BodyRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()
    private val attribute = Attribute(12, "Stuff")

    @Test
    fun show() {
        val result1 = TestResult("12.8", ReferenceRange("0.5", "50"), "mmol/L")
        val result2 = TestResult("41.0", ReferenceRange("10", "40"), "mmol/L")

        val results = listOf( result1, result2)
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.attributeColumnWeight }.returns(0.2F)
        every { columnWidths.valueColumnWeight() }.returns(0.3F)
        every { columnWidths.referenceRangeColumnWeight }.returns(0.2F)
        composeTestRule.setContent {
            BodyRow( 5, attribute, columnWidths, results)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(attribute.name))
            waitUntilExactlyOneExists(hasText(resultText(result1)))
            waitUntilExactlyOneExists(hasText(resultText(result2)))
            waitUntilExactlyOneExists(hasText(rangeText(result2.referenceRange)))
        }
    }
}