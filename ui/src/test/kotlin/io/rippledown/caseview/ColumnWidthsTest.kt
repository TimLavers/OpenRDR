package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import org.junit.Test

class ColumnWidthsTest {

    @Test
    fun attributeColumnWeight() {
        ColumnWidths(1).attributeColumnWeight shouldBe 0.2F
        ColumnWidths(10).attributeColumnWeight shouldBe 0.2F
    }

    @Test
    fun referenceRangeColumnWeight() {
        ColumnWidths(1).referenceRangeColumnWeight shouldBe 0.2F
        ColumnWidths(10).referenceRangeColumnWeight shouldBe 0.2F
    }

    @Test
    fun valueColumnWeight() {
        ColumnWidths(1).valueColumnWeight() shouldBe 0.6F
        ColumnWidths(10).valueColumnWeight() shouldBe 0.06F
        ColumnWidths(10).valueColumnWeight() shouldBe 0.06F
    }
}