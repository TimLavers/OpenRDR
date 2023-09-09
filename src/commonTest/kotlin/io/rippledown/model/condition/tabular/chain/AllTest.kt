package io.rippledown.model.condition.tabular.chain

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AllTest: ChainTestBase() {

    private val all = All

    @Test
    fun evaluateEmptySequence() {
        // This is not the same as in set theory, but make
        // sense in terms of natural language.
        all.matches(emptyList()) shouldBe false
    }

    @Test
    fun evaluateLength1() {
        all.matches(t) shouldBe true
        all.matches(f) shouldBe false
    }

    @Test
    fun evaluateLength2() {
        all.matches(tt) shouldBe true
        all.matches(tf) shouldBe false
        all.matches(ft) shouldBe false
        all.matches(ff) shouldBe false
    }

    @Test
    fun evaluateLengthySequence() {
        all.matches(listOf(true, true, true, true, true)) shouldBe true
        all.matches(listOf(true, true, true, true, true)) shouldBe true
        all.matches(listOf(true, false, false, false, false)) shouldBe false
        all.matches(listOf(false, false, true, false, false)) shouldBe false
    }

    @Test
    fun serialization() {
        serializeDeserialize(all) shouldBe all
    }

    @Test
    fun description() {
        all.description() shouldBe "all"
    }

    @Test
    fun plurality() {
        all.plurality() shouldBe true
    }
}