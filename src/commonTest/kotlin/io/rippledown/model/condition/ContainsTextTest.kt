package io.rippledown.model.condition

import io.kotest.matchers.*
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import kotlin.test.Test

internal class ContainsTextTest: ConditionTestBase() {

    private val condition = ContainsText(88, tsh, "goat")

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as ContainsText
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId) as ContainsText
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
        alignedCopy.toFind shouldBe condition.toFind
    }

    @Test
    fun id() {
        condition.id shouldBe 88
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(ContainsText(100, condition.attribute, condition.toFind))
        condition should beSameAs(ContainsText(null, condition.attribute, condition.toFind))

        condition shouldNot beSameAs(Is(null, condition.attribute, "horse"))
        condition shouldNot beSameAs(ContainsText(null, condition.attribute, "horse"))
        condition shouldNot beSameAs(ContainsText(condition.id, condition.attribute, "horse"))
        condition shouldNot beSameAs(ContainsText(null, glucose, condition.toFind))
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
        val containsText = ContainsText(90, clinicalNotes, "goat")
        containsText.holds(createCase("")) shouldBe false
        containsText.holds(createCase("sheep")) shouldBe false
        containsText.holds(createCase("goat")) shouldBe true
        containsText.holds(createCase("goats")) shouldBe true
        containsText.holds(createCase("Goat")) shouldBe false
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
        condition.asText() shouldBe "TSH contains \"goat\""
    }
}