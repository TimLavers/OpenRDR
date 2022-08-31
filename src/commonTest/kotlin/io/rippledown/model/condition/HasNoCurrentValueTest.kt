package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlin.test.Test

internal class HasNoCurrentValueTest: ConditionTestBase() {

    private val condition = HasNoCurrentValue(tsh)

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe true
    }

    @Test
    fun currentValueBlank() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe true
    }

    @Test
    fun currentValueNumeric() {
        condition.holds(twoEpisodeCaseWithFirstTSHValueBlank()) shouldBe false
    }

    @Test
    fun currentValueNonNumeric() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe false
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        condition.asText() shouldBe "TSH has no current value"
        HasNoCurrentValue(Attribute("Blah !@#@#")).asText() shouldBe "Blah !@#@# has no current value"
    }
}