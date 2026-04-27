package io.rippledown.caseview

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class BodyRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val attribute = Attribute(12, "Stuff")
    private val result1 = Result("12.8", ReferenceRange("0.5", "50"), "mmol/L")
    private val result2 = Result("41.0", ReferenceRange("10", "40"), "mmol/L")
    private val results = listOf( result1, result2)
    private val columnWidths = mockk<ColumnWidths>()

    init {
        every { columnWidths.attributeColumnWeight }.returns(0.2F)
        every { columnWidths.valueColumnWeight() }.returns(0.2F)
        every { columnWidths.valueRangeGapWeight }.returns(0.1F)
        every { columnWidths.referenceRangeColumnWeight }.returns(0.2F)
        every { columnWidths.unitsColumnWeight }.returns(0.1F)
        every { columnWidths.scrollableAreaWeight() }.returns(0.3F)
    }

    @Test
    fun show() {
        composeTestRule.setContent {
            BodyRow(5, "Bondi", attribute, columnWidths, results)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(attribute.name))
            // Value text and units render as sibling Text nodes, so look up
            // each component separately.
            waitUntilExactlyOneExists(hasText(result1.value.text))
            waitUntilExactlyOneExists(hasText(result2.value.text))
            waitUntilExactlyOneExists(hasText(rangeText(result2.referenceRange)))
            // Units are rendered in their own dedicated cell (one per row, on
            // the right). result2's units win because units are taken from
            // the last result.
            waitUntilExactlyOneExists(hasText(result2.units!!))
        }
    }

    @Test
    fun displacement() {
        with(composeTestRule) {
            setContent {
                BodyRow(5, "Bondi", attribute, columnWidths, results)
            }
            val boundsNoDisplacement = onNodeWithText(attribute.name).getBoundsInRoot()
            setContent {
                BodyRow(5, "Bondi", attribute, columnWidths, results, 123F)
            }
            val boundsWithDisplacement = onNodeWithText(attribute.name).getBoundsInRoot()
            boundsWithDisplacement.top  - boundsNoDisplacement.top shouldBe 123.dp
        }
    }
}