package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.Attribute
import io.rippledown.model.beSameAs
import kotlin.test.Test

internal class GreaterThanOrEqualToTest: ConditionTestBase() {

    private val gte1 = GreaterThanOrEqualTo(99, tsh, 1.01)

    @Test
    fun attributeNotInCase() {
        gte1.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun id() {
        gte1.id shouldBe 99
    }

    @Test
    fun sameAs() {
        gte1 should beSameAs(gte1)
        gte1 should beSameAs(GreaterThanOrEqualTo(100, gte1.attribute, gte1.d))
        gte1 should beSameAs(GreaterThanOrEqualTo(null, gte1.attribute, gte1.d))

        gte1 shouldNot beSameAs(LessThanOrEqualTo(null, gte1.attribute, gte1.d))
        gte1 shouldNot beSameAs(GreaterThanOrEqualTo(null, gte1.attribute, 1.02))
        gte1 shouldNot beSameAs(GreaterThanOrEqualTo(gte1.id, gte1.attribute, 1.02))
        gte1 shouldNot beSameAs(GreaterThanOrEqualTo(null, glucose, gte1.d))
    }

    @Test
    fun holds() {
        val height = Attribute("Height", 100)
        val gte = GreaterThanOrEqualTo(100, height, 1.8)
        gte.holds(twoEpisodeCase(height, "1.78", "1.2")) shouldBe false
        gte.holds(twoEpisodeCase(height, "1.78", "1.799")) shouldBe false
        gte.holds(twoEpisodeCase(height, "1.78", "1.801")) shouldBe true
        gte.holds(twoEpisodeCase(height, "1.78", "1.9")) shouldBe true
        gte.holds(twoEpisodeCase(height, "1.9", "1.0")) shouldBe false
    }

    @Test
    fun valueNonNumeric() {
        gte1.holds(tshValueNonNumericCase()) shouldBe false
        gte1.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
        gte1.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe false
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(gte1) shouldBe gte1
    }

    @Test
    fun asText() {
        // In the JVM we get "TSH ≥ 1.0" but in JS get "TSH ≥ 1".
        // This is because JS has just a single number type.
        gte1.asText() shouldContain "TSH ≥ 1"
    }
}