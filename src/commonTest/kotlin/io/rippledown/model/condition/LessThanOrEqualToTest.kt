package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.Attribute
import kotlin.test.Test

internal class LessThanOrEqualToTest: ConditionTestBase() {

    private val lte = LessThanOrEqualTo(tsh, 1.01)

    @Test
    fun attributeNotInCase() {
        lte.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun holds() {
        val height = Attribute("Height", 100)
        val lte2 = LessThanOrEqualTo(height, 1.8)
        lte2.holds(twoEpisodeCase(height, "1.78", "1.2")) shouldBe true
        lte2.holds(twoEpisodeCase(height, "1.78", "1.799")) shouldBe true
        lte2.holds(twoEpisodeCase(height, "1.78", "1.801")) shouldBe false
        lte2.holds(twoEpisodeCase(height, "1.78", "1.9")) shouldBe false
        lte2.holds(twoEpisodeCase(height, "1.9", "1.0")) shouldBe true
    }

    @Test
    fun valueNonNumeric() {
        lte.holds(tshValueNonNumericCase()) shouldBe false
        lte.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
        lte.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe false
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(lte) shouldBe lte
    }

    @Test
    fun asText() {
        // In the JVM we get "TSH ≤ 1.0" but in JS get "TSH ≤ 1".
        // This is because JS has just a single number type.
        lte.asText() shouldContain "TSH ≤ 1"
    }
}