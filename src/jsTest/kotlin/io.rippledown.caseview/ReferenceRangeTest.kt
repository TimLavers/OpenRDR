package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import kotlin.test.Test

class ReferenceRangeTest {
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