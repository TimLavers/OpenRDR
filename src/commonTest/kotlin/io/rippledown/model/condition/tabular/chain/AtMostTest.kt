package io.rippledown.model.condition.tabular.chain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class AtMostTest: ChainTestBase() {

    private val atMost = AtMost(3)

    @Test
    fun evaluateEmptySequence() {
        atMost.matches(emptyList()) shouldBe true
    }

    @Test
    fun evaluateLength() {
        atMost.matches(t) shouldBe true
        atMost.matches(f) shouldBe true

        atMost.matches(tt) shouldBe true
        atMost.matches(tf) shouldBe true
        atMost.matches(ft) shouldBe true
        atMost.matches(ff) shouldBe true

        atMost.matches(listOf(true, true, true)) shouldBe true
        atMost.matches(listOf(true, true, false)) shouldBe true
        atMost.matches(listOf(true, false, true)) shouldBe true
        atMost.matches(listOf(true, false, false)) shouldBe true

        atMost.matches(listOf(true, true, true, true)) shouldBe false
        atMost.matches(listOf(false, true, true, false)) shouldBe true
        atMost.matches(listOf(false, true, true, true)) shouldBe true

        atMost.matches(listOf(false, true, false, true, false, true, false, true)) shouldBe false
    }

    @Test
    fun equality() {
        AtMost(5) shouldBe AtMost(5)
        AtMost(5) shouldNotBe AtMost(6)
    }

    @Test
    fun hash() {
        AtMost(5).hashCode() shouldBe AtMost(5).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(atMost) shouldBe AtMost(3)
    }

    @Test
    fun description() {
        atMost.description() shouldBe "at most 3"
    }

    @Test
    fun plurality() {
        AtLeast(1).plurality() shouldBe false
        AtLeast(2).plurality() shouldBe true
        AtLeast(3).plurality() shouldBe true
    }
}