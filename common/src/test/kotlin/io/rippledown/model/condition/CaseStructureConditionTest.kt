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

    val userExpression = "TSH was not tested"
    private val tshAbsent = CaseStructureCondition(123, IsAbsentFromCase(tsh), userExpression)

    @Test
    fun attributeNames() {
        tshAbsent.attributeNames() shouldBe setOf(tsh.name)
    }

    @Test
    fun holds() {
        tshAbsent.holds(multiEpisodeTSHCase("1", "2", "3")) shouldBe false
        tshAbsent.holds(multiEpisodeTSHCase("1", "2", "0")) shouldBe false
        tshAbsent.holds(clinicalNotesCase("1")) shouldBe true
    }

    @Test
    fun asText() {
        tshAbsent.asText() shouldBe "${tsh.name} is not in case"
    }

    @Test
    fun userExpression() {
        tshAbsent.userExpression shouldBe userExpression
    }

    @Test
    fun id() {
        tshAbsent.id shouldBe 123
    }

    @Test
    fun serialization() {
        serializeDeserialize(tshAbsent) shouldBe tshAbsent

        checkSerializationIsThreadSafe(tshAbsent)
    }

    @Test
    fun `serialization without id`() {
        val idLess = CaseStructureCondition(null, IsPresentInCase(tsh))
        serializeDeserialize(idLess) shouldBe idLess
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
        tshAbsent.sameAs(CaseStructureCondition(null, IsAbsentFromCase(tsh), userExpression)) shouldBe true
        tshAbsent.sameAs(CaseStructureCondition(99, IsAbsentFromCase(tsh), userExpression)) shouldBe true

        // Same but for user expression.
        tshAbsent.sameAs(CaseStructureCondition(null, IsAbsentFromCase(tsh), "tsh not found")) shouldBe true
        tshAbsent.sameAs(CaseStructureCondition(99, IsAbsentFromCase(tsh), "tsh not found")) shouldBe true

        // Predicate different.
        tshAbsent.sameAs(CaseStructureCondition(99, IsPresentInCase(clinicalNotes), userExpression)) shouldBe false
    }
}