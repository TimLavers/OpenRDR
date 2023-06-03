package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.Attribute
import io.rippledown.model.beSameAs
import kotlin.test.Test

internal class GreaterThanOrEqualToTest: ConditionTestBase() {

    private val condition = GreaterThanOrEqualTo(99, tsh, 1.01)

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun id() {
        condition.id shouldBe 99
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as GreaterThanOrEqualTo
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
        alignedCopy.d shouldBe condition.d
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(GreaterThanOrEqualTo(100, condition.attribute, condition.d))
        condition should beSameAs(GreaterThanOrEqualTo(null, condition.attribute, condition.d))

        condition shouldNot beSameAs(LessThanOrEqualTo(null, condition.attribute, condition.d))
        condition shouldNot beSameAs(GreaterThanOrEqualTo(null, condition.attribute, 1.02))
        condition shouldNot beSameAs(GreaterThanOrEqualTo(condition.id, condition.attribute, 1.02))
        condition shouldNot beSameAs(GreaterThanOrEqualTo(null, glucose, condition.d))
    }

    @Test
    fun holds() {
        val height = Attribute(100, "Height")
        val gte = GreaterThanOrEqualTo(100, height, 1.8)
        gte.holds(twoEpisodeCase(height, "1.78", "1.2")) shouldBe false
        gte.holds(twoEpisodeCase(height, "1.78", "1.799")) shouldBe false
        gte.holds(twoEpisodeCase(height, "1.78", "1.801")) shouldBe true
        gte.holds(twoEpisodeCase(height, "1.78", "1.9")) shouldBe true
        gte.holds(twoEpisodeCase(height, "1.9", "1.0")) shouldBe false
    }

    @Test
    fun valueNonNumeric() {
        condition.holds(tshValueNonNumericCase()) shouldBe false
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
        condition.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe false
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        // In the JVM we get "TSH ≥ 1.0" but in JS get "TSH ≥ 1".
        // This is because JS has just a single number type.
        condition.asText() shouldContain "TSH ≥ 1"
    }
}