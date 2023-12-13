package io.rippledown.model.condition.episodic.signature

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NoTest: ChainTestBase() {

    private val no = No

    @Test
    fun evaluateEmptySequence() {
        no.matches(emptyList()) shouldBe true
    }

    @Test
    fun evaluateLength1() {
        no.matches(t) shouldBe false
        no.matches(f) shouldBe true
    }

    @Test
    fun evaluateLength2() {
        no.matches(tt) shouldBe false
        no.matches(tf) shouldBe false
        no.matches(ft) shouldBe false
        no.matches(ff) shouldBe true
    }

    @Test
    fun evaluateLengthySequence() {
        no.matches(listOf(true, true, true, true, true)) shouldBe false
        no.matches(listOf(true, true, true, true, true)) shouldBe false
        no.matches(listOf(true, false, false, false, false)) shouldBe false
        no.matches(listOf(false, false, false, false, false)) shouldBe true
    }

    @Test
    fun serialization() {
        serializeDeserialize(no) shouldBe no
    }

    @Test
    fun description() {
        no.description() shouldBe "no"
    }

    @Test
    fun plurality() {
        no.plurality() shouldBe false
    }
}