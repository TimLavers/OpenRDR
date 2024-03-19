package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.mocks.DummyRowScope
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
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
        val testResult = TestResult("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, testResult, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8"))
        }
    }

    @Test
    fun `column widths`() {
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.valueColumnWeight() }.returns(0.5F)

        val testResult = TestResult("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, testResult, columnWidths)
        }
    }

    @Test
    fun `show result that has units`() {
        val testResult = TestResult("12.8", null, "waves / sec")
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell("Bondi", tsh, 1, testResult, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8 waves / sec"))
        }
    }

    @Test
    fun resultTextTest() {
        resultText(TestResult("44.1", null, null)) shouldBe "44.1"
        resultText(TestResult("44.1", ReferenceRange("1", "2"), null)) shouldBe "44.1"
        resultText(TestResult("44.1", null, "furlongs / fortnight")) shouldBe "44.1 furlongs / fortnight"
        resultText(TestResult("10", null, "mmol/L")) shouldBe "10 mmol/L"
        resultText(TestResult("10", null, " mmol/L ")) shouldBe "10 mmol/L"
    }
}

