package io.rippledown.caseview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
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
    private val result1 = TestResult("12.8", ReferenceRange("0.5", "50"), "mmol/L")
    private val result2 = TestResult("41.0", ReferenceRange("10", "40"), "mmol/L")
    private val results = listOf( result1, result2)
    private val columnWidths = mockk<ColumnWidths>()

    init {
        every { columnWidths.attributeColumnWeight }.returns(0.2F)
        every { columnWidths.valueColumnWeight() }.returns(0.3F)
        every { columnWidths.referenceRangeColumnWeight }.returns(0.2F)
    }

    @Test
    fun show() {
        composeTestRule.setContent {
            BodyRow(5, "Bondi", attribute, columnWidths, results)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(attribute.name))
            waitUntilExactlyOneExists(hasText(resultText(result1)))
            waitUntilExactlyOneExists(hasText(resultText(result2)))
            waitUntilExactlyOneExists(hasText(rangeText(result2.referenceRange)))
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
            println("bounds: $boundsWithDisplacement")
            boundsWithDisplacement.top  - boundsNoDisplacement.top shouldBe 123.dp
        }
    }
}