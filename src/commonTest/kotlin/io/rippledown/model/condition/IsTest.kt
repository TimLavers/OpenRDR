package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import kotlin.test.Test

internal class IsTest: ConditionTestBase() {

    private val condition = Is(1000, tsh, "goat")

    @Test
    fun id() {
        condition.id shouldBe 1000
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as Is
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
        alignedCopy.toFind shouldBe condition.toFind
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(Is(100, condition.attribute, condition.toFind))
        condition should beSameAs(Is(null, condition.attribute, condition.toFind))

        condition shouldNot beSameAs(ContainsText(condition.id, condition.attribute, condition.toFind))
        condition shouldNot beSameAs(Is(null, glucose, condition.toFind))
        condition shouldNot beSameAs(Is(condition.id, glucose, condition.toFind))
        condition shouldNot beSameAs(Is(condition.id, glucose, condition.toFind))
        condition shouldNot beSameAs(Is(null, glucose, condition.toFind))
    }

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun currentValueBlank() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
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
    fun holds() {
        val isText = Is(89, clinicalNotes, "goat")
        isText.holds(createCase("sheep")) shouldBe false
        isText.holds(createCase("goat")) shouldBe true
        isText.holds(createCase("Goat")) shouldBe false
    }

    @Test
    fun nonAscii() {
        val isText = Is(89, clinicalNotes, "<5 pmol/L")
        isText.holds(createCase("<5 pmol/L")) shouldBe true
    }

    private fun createCase(notes: String): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(clinicalNotes, defaultDate , TestResult(notes))
        return builder1.build("Case")
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        condition.asText() shouldBe "TSH is \"goat\""
    }
}