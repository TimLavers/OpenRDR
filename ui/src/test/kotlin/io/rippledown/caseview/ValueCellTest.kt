package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.mocks.DummyRowScope
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ValueCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val tsh = Attribute(12, "TSH")
    private val columnWidths = ColumnWidths(7)

    @Test
    fun `show result that does not have units`() {
        val Result = Result("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8"))
        }
    }

    @Test
    fun `column widths`() {
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.valueColumnWeight() }.returns(0.5F)

        val Result = Result("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
    }

    @Test
    fun `value cell shows only the numeric value, not the units`() {
        // Units are rendered in the dedicated UnitsCell (see BodyRow), not
        // inside ValueCell, so a result with units should still display only
        // the numeric value here.
        val Result = Result("12.8", null, "waves / sec")
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8"))
            onAllNodesWithText("waves / sec", substring = true).assertCountEquals(0)
        }
    }

    @Test
    fun `show out-of-range marker when value is above the upper bound`() {
        val Result = Result("12.8", ReferenceRange("1", "10"), null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8"))
            waitUntilExactlyOneExists(hasText("*"))
        }
    }

    @Test
    fun `show out-of-range marker when value is below the lower bound`() {
        val Result = Result("0.5", ReferenceRange("1", "10"), null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("0.5"))
            waitUntilExactlyOneExists(hasText("*"))
        }
    }

    @Test
    fun `do not show marker when value is within the reference range`() {
        val Result = Result("5.0", ReferenceRange("1", "10"), null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("5.0"))
            onAllNodes(hasText("*")).assertCountEquals(0)
        }
    }

    @Test
    fun `do not show marker when there is no reference range`() {
        val Result = Result("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, Result, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8"))
            onAllNodes(hasText("*")).assertCountEquals(0)
        }
    }

    @Test
    fun resultTextTest() {
        resultText(Result("44.1", null, null)) shouldBe "44.1"
        resultText(Result("44.1", ReferenceRange("1", "2"), null)) shouldBe "44.1"
        resultText(Result("44.1", null, "furlongs / fortnight")) shouldBe "44.1 furlongs / fortnight"
        resultText(Result("10", null, "mmol/L")) shouldBe "10 mmol/L"
        resultText(Result("10", null, " mmol/L ")) shouldBe "10 mmol/L"
    }
}

