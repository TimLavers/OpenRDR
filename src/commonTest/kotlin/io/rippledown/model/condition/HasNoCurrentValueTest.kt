package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import kotlin.test.Test

internal class HasNoCurrentValueTest: ConditionTestBase() {

    private val condition = HasNoCurrentValue(78, tsh)

    @Test
    fun id() {
        condition.id shouldBe 78
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as HasNoCurrentValue
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(HasNoCurrentValue(100, condition.attribute))
        condition should beSameAs(HasNoCurrentValue(null, condition.attribute))

        condition shouldNot beSameAs(Is(null, condition.attribute, "horse"))
        condition shouldNot beSameAs(HasNoCurrentValue(null, glucose))
        condition shouldNot beSameAs(HasNoCurrentValue(condition.id, glucose))
    }

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
        HasNoCurrentValue(100, Attribute("Blah !@#@#", 100)).asText() shouldBe "Blah !@#@# has no current value"
    }
}