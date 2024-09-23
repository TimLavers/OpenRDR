package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.checkSerializationIsThreadSafe
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class CaseStructureConditionTest: ConditionTestBase() {

    private val tshAbsent = CaseStructureCondition(123, IsAbsentFromCase(tsh))

    @Test
    fun holds() {
        tshAbsent.holds(multiEpisodeTSHCase("1", "2", "3")) shouldBe false
        tshAbsent.holds(multiEpisodeTSHCase("1", "2", "0")) shouldBe false
        tshAbsent.holds(clinicalNotesCase("1")) shouldBe true
    }

    @Test
    fun description() {
        tshAbsent.asText() shouldBe "${tsh.name} is not in case"
    }

    @Test
    fun id() {
        tshAbsent.id shouldBe 123
    }

    @Test
    fun serialization() {
        serializeDeserialize(tshAbsent) shouldBe tshAbsent

        // One without an id.
        val idLess = CaseStructureCondition(null, IsPresentInCase(tsh))
        serializeDeserialize(idLess) shouldBe idLess

        checkSerializationIsThreadSafe(tshAbsent)
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(tshAbsent)
        conditionCopy.predicate shouldNotBeSameInstanceAs tshAbsent.predicate
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        (alignedCopy.predicate as IsAbsentFromCase).attribute shouldBeSameInstanceAs tsh
    }

    @Test
    fun sameAs() {
        // Identical.
        tshAbsent.sameAs(tshAbsent) shouldBe true
        tshAbsent.sameAs(tshAbsent.copy()) shouldBe true

        // Same but for id.
        tshAbsent.sameAs(CaseStructureCondition(null, IsAbsentFromCase(tsh))) shouldBe true
        tshAbsent.sameAs(CaseStructureCondition(99, IsAbsentFromCase(tsh))) shouldBe true

        // Predicate different.
        tshAbsent.sameAs(CaseStructureCondition(99, IsPresentInCase(clinicalNotes))) shouldBe false
    }
}