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
        ColumnWidths(1).valueColumnWeight() shouldBe (0.16F plusOrMinus 1e-6F)
        ColumnWidths(10).valueColumnWeight() shouldBe (0.016F plusOrMinus 1e-6F)
    }
}