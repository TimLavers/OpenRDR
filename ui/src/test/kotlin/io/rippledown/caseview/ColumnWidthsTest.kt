package io.rippledown.caseview

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test

class ColumnWidthsTest {

    @Test
    fun attributeColumnWeight() {
        ColumnWidths(1).attributeColumnWeight shouldBe 0.3F
        ColumnWidths(10).attributeColumnWeight shouldBe 0.3F
    }

    @Test
    fun referenceRangeColumnWeight() {
        ColumnWidths(1).referenceRangeColumnWeight shouldBe 0.2F
        ColumnWidths(10).referenceRangeColumnWeight shouldBe 0.2F
    }

    @Test
    fun valueColumnWeight() {
        ColumnWidths(1).valueColumnWeight() shouldBe (0.5F plusOrMinus 1e-6F)
        ColumnWidths(10).valueColumnWeight() shouldBe (0.05F plusOrMinus 1e-6F)
    }
}