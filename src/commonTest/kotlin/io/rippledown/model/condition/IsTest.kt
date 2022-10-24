package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlin.test.Test

internal class IsTest: ConditionTestBase() {

    private val condition = Is(tsh, "goat")

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
        val isText = Is(clinicalNotes, "goat")
        isText.holds(createCase("sheep")) shouldBe false
        isText.holds(createCase("goat")) shouldBe true
        isText.holds(createCase("Goat")) shouldBe false
    }

    @Test
    fun nonAscii() {
        val isText = Is(clinicalNotes, "<5 pmol/L")
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