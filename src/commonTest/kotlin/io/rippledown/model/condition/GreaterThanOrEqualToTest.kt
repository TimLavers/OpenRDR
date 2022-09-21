package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlin.test.Test

internal class GreaterThanOrEqualToTest: ConditionTestBase() {

    private val gte1 = GreaterThanOrEqualTo(tsh, 1.0)

    @Test
    fun attributeNotInCase() {
        gte1.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun holds() {
        val height = Attribute("Height")
        val gte = GreaterThanOrEqualTo(height, 1.8)
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
        gte1.asText() shouldBe "TSH ≥ 1.0"
    }
}