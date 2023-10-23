package io.rippledown.model.condition.episodic.signature

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CurrentTest: ChainTestBase() {

    private val current = Current

    @Test
    fun evaluateEmptySequence() {
        current.matches(emptyList()) shouldBe false
    }

    @Test
    fun evaluateLength1() {
        current.matches(t) shouldBe true
        current.matches(f) shouldBe false
    }

    @Test
    fun evaluateLength2() {
        current.matches(tt) shouldBe true
        current.matches(tf) shouldBe false
        current.matches(ft) shouldBe true
        current.matches(ff) shouldBe false
    }

    @Test
    fun evaluateLengthySequence() {
        current.matches(listOf(false, false, true, false, true)) shouldBe true
        current.matches(listOf(false, false, false, false, true)) shouldBe true
        current.matches(listOf(false, false, true, false, false)) shouldBe false
    }

    @Test
    fun serialization() {
        serializeDeserialize(current) shouldBe current
    }

    @Test
    fun description() {
        current.description() shouldBe ""
    }

    @Test
    fun plurality() {
        current.plurality() shouldBe false
    }
}