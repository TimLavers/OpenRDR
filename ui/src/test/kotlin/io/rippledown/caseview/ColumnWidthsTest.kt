package io.rippledown.caseview

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test

class ColumnWidthsTest {

    @Test
    fun attributeColumnWeight() {
        ColumnWidths(1).attributeColumnWeight shouldBe 0.28F
        ColumnWidths(10).attributeColumnWeight shouldBe 0.28F
    }

    @Test
    fun valueRangeGapWeight() {
        ColumnWidths(1).valueRangeGapWeight shouldBe 0.24F
        ColumnWidths(10).valueRangeGapWeight shouldBe 0.24F
    }

    @Test
    fun referenceRangeColumnWeight() {
        ColumnWidths(1).referenceRangeColumnWeight shouldBe 0.20F
        ColumnWidths(10).referenceRangeColumnWeight shouldBe 0.20F
    }

    @Test
    fun unitsColumnWeight() {
        ColumnWidths(1).unitsColumnWeight shouldBe 0.12F
        ColumnWidths(10).unitsColumnWeight shouldBe 0.12F
    }

    @Test
    fun valueColumnWeight() {
        // Per-episode value column weight is constant regardless of how many
        // episodes the case has; the table simply grows wider as more
        // episodes are added.
        ColumnWidths(1).valueColumnWeight() shouldBe (0.16F plusOrMinus 1e-6F)
        ColumnWidths(10).valueColumnWeight() shouldBe (0.16F plusOrMinus 1e-6F)
    }

    @Test
    fun scrollableAreaWeight() {
        // Width of the horizontally scrollable date/value area:
        // valueColumnWeight (0.16) + valueRangeGapWeight (0.24) = 0.40.
        // It is independent of the number of episodes — the inner scroll
        // content grows wider per episode while this slot stays the same
        // proportional width of the panel.
        ColumnWidths(1).scrollableAreaWeight() shouldBe (0.40F plusOrMinus 1e-6F)
        ColumnWidths(5).scrollableAreaWeight() shouldBe (0.40F plusOrMinus 1e-6F)
    }
}