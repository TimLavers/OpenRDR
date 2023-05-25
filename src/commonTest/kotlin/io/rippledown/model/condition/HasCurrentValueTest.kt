package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.defaultDate
import kotlin.test.Test

internal class HasCurrentValueTest : ConditionTestBase() {

    private val condition = HasCurrentValue(tsh)

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun currentValueBlank() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
    }

    @Test
    fun currentValuePresent() {
        condition.holds(twoEpisodeCaseWithFirstTSHValueBlank()) shouldBe true
    }

    @Test
    fun currentValueNotPresent() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
    }

    @Test
    fun holds() {
        val hasCurrentValue = HasCurrentValue(clinicalNotes)
        hasCurrentValue.holds(createCase("sheep")) shouldBe true
        hasCurrentValue.holds(createCase("")) shouldBe false
    }

    @Test
    fun nonAscii() {
        val hasCurrentValue = HasCurrentValue(clinicalNotes)
        hasCurrentValue.holds(createCase("<5 pmol/L")) shouldBe true
    }

    private fun createCase(notes: String): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addResult(clinicalNotes, defaultDate, TestResult(notes))
        return builder.build("Case")
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        condition.asText() shouldBe "TSH has a current value"
    }
}