package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import kotlin.test.Test

internal class HasCurrentValueTest: ConditionTestBase() {

    private val condition = HasCurrentValue(78, tsh)

    @Test
    fun id() {
        condition.id shouldBe 78
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as HasCurrentValue
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(HasCurrentValue(100, condition.attribute))
        condition should beSameAs(HasCurrentValue(null, condition.attribute))

        condition shouldNot beSameAs(Is(null, condition.attribute, "horse"))
        condition shouldNot beSameAs(HasCurrentValue(null, glucose))
        condition shouldNot beSameAs(HasCurrentValue(condition.id, glucose))
    }

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun holds() {
        val hasCurrentValue = HasCurrentValue(clinicalNotes)
        hasCurrentValue.holds(createCase("sheep")) shouldBe true
        hasCurrentValue.holds(createCase("")) shouldBe false
    }

    @Test
    fun nonAscii() {
        val hasCurrentValue = HasCurrentValue(clinicalNotes)
        hasCurrentValue.holds(createCase("<5 pmol/L")) shouldBe true
    }

    @Test
    fun currentValueBlank() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
    }

    @Test
    fun currentValueNumeric() {
        condition.holds(twoEpisodeCaseWithFirstTSHValueBlank()) shouldBe true
    }

    @Test
    fun currentValueNonNumeric() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe true
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        condition.asText() shouldBe "TSH has a current value"
        HasCurrentValue(100, Attribute("Blah !@#@#", 100)).asText() shouldBe "Blah !@#@# has a current value"
    }
}