package io.rippledown.caseview

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.OUT_OF_RANGE_MARKER_TEXT
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class OutOfRangeMarkerTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val tsh = Attribute(12, "TSH")
    private val caseName = "Bondi"

    @Test
    fun `renders the marker text`() {
        composeTestRule.setContent {
            OutOfRangeMarker(caseName, tsh)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(OUT_OF_RANGE_MARKER_TEXT))
        }
    }

    @Test
    fun `exposes a content description that identifies the case and attribute`() {
        composeTestRule.setContent {
            OutOfRangeMarker(caseName, tsh)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(
                hasContentDescription(outOfRangeMarkerContentDescription(caseName, tsh.name))
            )
        }
    }

    @Test
    fun `content description helper combines the case and attribute names`() {
        outOfRangeMarkerContentDescription("Bondi", "TSH") shouldBe
                "Out of range marker for Bondi TSH"
    }

    @Test
    fun `Result is high when its value exceeds the upper bound`() {
        Result("12.8", ReferenceRange("1", "10"), null).isHigh() shouldBe true
        Result("12.8", ReferenceRange("1", "10"), null).isLow() shouldBe false
        Result("12.8", ReferenceRange("1", "10"), null).isOutOfRange() shouldBe true
    }

    @Test
    fun `Result is low when its value is below the lower bound`() {
        Result("0.5", ReferenceRange("1", "10"), null).isHigh() shouldBe false
        Result("0.5", ReferenceRange("1", "10"), null).isLow() shouldBe true
        Result("0.5", ReferenceRange("1", "10"), null).isOutOfRange() shouldBe true
    }

    @Test
    fun `Result is in range when its value is between the bounds`() {
        val result = Result("5.0", ReferenceRange("1", "10"), null)
        result.isHigh() shouldBe false
        result.isLow() shouldBe false
        result.isOutOfRange() shouldBe false
    }

    @Test
    fun `Result with no reference range is never out of range`() {
        val result = Result("12.8", null, null)
        result.isHigh() shouldBe false
        result.isLow() shouldBe false
        result.isOutOfRange() shouldBe false
    }

    @Test
    fun `Result with non-numeric value is never out of range`() {
        // The reference range helpers gate on a numeric (`real`) value, so a
        // textual result should never be flagged as out of range even when a
        // range is present.
        val result = Result("Fasting", ReferenceRange("1", "10"), null)
        result.isHigh() shouldBe false
        result.isLow() shouldBe false
        result.isOutOfRange() shouldBe false
    }
}
