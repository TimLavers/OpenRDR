package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.mocks.DummyRowScope
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReferenceRangeCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()
    private val tsh = Attribute(12, "TSH")

    @Test
    fun `show result that does not have units`() {
        val testResult = TestResult("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ReferenceRangeCell( tsh, testResult, 0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(""))
        }
    }

    @Test
    fun `show result with two-sided range`() {
        val testResult = TestResult("12.8", ReferenceRange("1.0", "2.5"), null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ReferenceRangeCell( tsh, testResult, 0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("1.0 - 2.5"))
        }
    }

    @Test
    fun shouldFormatNullRange() {
        rangeText(null) shouldBe ""
    }

    @Test
    fun shouldFormatTwoSidedRange() {
        rangeText(ReferenceRange("1", "2")) shouldBe "1 - 2"
    }

    @Test
    fun shouldFormatOneSidedLowRange() {
        rangeText(ReferenceRange("1", null)) shouldBe "> 1"
    }

    @Test
    fun shouldFormatOneSidedHighRange() {
        rangeText(ReferenceRange(null, "2")) shouldBe "< 2"
    }
}

