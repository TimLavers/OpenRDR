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

internal class LessThanOrEqualToTest: ConditionTestBase() {

    private val lte = LessThanOrEqualTo(344, tsh, 1.01)

    @Test
    fun id() {
        lte.id shouldBe 344
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(lte) as LessThanOrEqualTo
        conditionCopy.attribute shouldNotBeSameInstanceAs lte.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs lte.attribute
        alignedCopy.d shouldBe lte.d
    }

    @Test
    fun sameAs() {
        lte should beSameAs(lte)
        lte should beSameAs(LessThanOrEqualTo(100, lte.attribute, lte.d))
        lte should beSameAs(LessThanOrEqualTo(null, lte.attribute, lte.d))

        lte shouldNot beSameAs(GreaterThanOrEqualTo(null, lte.attribute, lte.d))
        lte shouldNot beSameAs(LessThanOrEqualTo(null, lte.attribute, 1.02))
        lte shouldNot beSameAs(LessThanOrEqualTo(lte.id, lte.attribute, 1.02))
        lte shouldNot beSameAs(LessThanOrEqualTo(null, glucose, lte.d))
        lte shouldNot beSameAs(LessThanOrEqualTo(lte.id, glucose, lte.d))
    }
    
    @Test
    fun attributeNotInCase() {
        lte.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun holds() {
        val height = Attribute("Height", 100)
        val lte2 = LessThanOrEqualTo(98, height, 1.8)
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