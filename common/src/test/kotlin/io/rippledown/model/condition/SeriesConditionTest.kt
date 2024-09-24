package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.checkSerializationIsThreadSafe
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class SeriesConditionTest: ConditionTestBase() {

    private val tshIncreasing = SeriesCondition(123, tsh, Increasing)

    @Test
    fun holds() {
        tshIncreasing.holds(multiEpisodeTSHCase("1", "2", "3")) shouldBe true
        tshIncreasing.holds(multiEpisodeTSHCase("1", "2", "0")) shouldBe false
    }

    @Test
    fun description() {
        tshIncreasing.asText() shouldBe "${tsh.name} increasing"
    }

    @Test
    fun id() {
        tshIncreasing.id shouldBe 123
    }

    @Test
    fun attributeNotInCase() {
        tshIncreasing.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun serialization() {
        serializeDeserialize(tshIncreasing) shouldBe tshIncreasing

        // One without an id.
        val idLess = SeriesCondition(null, tsh, Increasing)
        serializeDeserialize(idLess) shouldBe idLess

        checkSerializationIsThreadSafe(tshIncreasing)
    }

    @Test
    fun asText() {
        tshIncreasing.asText() shouldBe "${tsh.name} increasing"
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(tshIncreasing)
        conditionCopy.attribute shouldNotBeSameInstanceAs tshIncreasing.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs tshIncreasing.attribute
    }

    @Test
    fun sameAs() {
        // Identical.
        tshIncreasing.sameAs(tshIncreasing) shouldBe true
        tshIncreasing.sameAs(tshIncreasing.copy()) shouldBe true

        // Same but for id.
        tshIncreasing.sameAs(SeriesCondition(null, tsh, Increasing)) shouldBe true
        tshIncreasing.sameAs(SeriesCondition(99, tsh, Increasing)) shouldBe true

        // Attribute different.
        tshIncreasing.sameAs(SeriesCondition(null, clinicalNotes, Increasing)) shouldBe false

        // Predicate different.
        tshIncreasing.sameAs(SeriesCondition(tshIncreasing.id, tsh, Decreasing)) shouldBe false
    }
}