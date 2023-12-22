package io.rippledown.model.condition.episodic.signature

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class AtLeastTest: ChainTestBase() {

    private val atLeast = AtLeast(3)

    @Test
    fun evaluateEmptySequence() {
        atLeast.matches(emptyList()) shouldBe false
    }

    @Test
    fun evaluateLength() {
        atLeast.matches(t) shouldBe false
        atLeast.matches(f) shouldBe false

        atLeast.matches(tt) shouldBe false
        atLeast.matches(tf) shouldBe false
        atLeast.matches(ft) shouldBe false
        atLeast.matches(ff) shouldBe false

        atLeast.matches(listOf(true, true, true)) shouldBe true
        atLeast.matches(listOf(true, true, false)) shouldBe false
        atLeast.matches(listOf(true, false, true)) shouldBe false
        atLeast.matches(listOf(true, false, false)) shouldBe false

        atLeast.matches(listOf(true, true, true, false)) shouldBe true
        atLeast.matches(listOf(false, true, true, false)) shouldBe false
        atLeast.matches(listOf(false, true, true, true)) shouldBe true

        atLeast.matches(listOf(false, true, false, true, false, true, false)) shouldBe true
    }

    @Test
    fun equality() {
        AtLeast(5) shouldBe AtLeast(5)
        AtLeast(5) shouldNotBe AtLeast(6)
    }

    @Test
    fun hash() {
        AtLeast(5).hashCode() shouldBe AtLeast(5).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(atLeast) shouldBe AtLeast(3)
    }

    @Test
    fun description() {
        atLeast.description() shouldBe "at least 3"
    }

    @Test
    fun plurality() {
        AtLeast(1).plurality() shouldBe false
        AtLeast(2).plurality() shouldBe true
        AtLeast(3).plurality() shouldBe true
    }
}