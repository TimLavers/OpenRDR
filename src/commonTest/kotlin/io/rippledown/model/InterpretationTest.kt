package io.rippledown.model

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.Rule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InterpretationTest {
    private val caseId = CaseId("1234", "Case 1")

    @Test
    fun construction() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        assertEquals(interpretation.caseId, caseId)
        assertEquals(interpretation.text, "Whatever, blah.")
    }

    @Test
    fun testEmpty() {
        Interpretation(caseId, "Whatever, blah.").conclusions().size shouldBe 0
    }

    @Test
    fun singleRule() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion("First conc")
        val rule = Rule(null, conclusion, emptySet())
        interpretation.add(rule)
        checkSingleConclusion(interpretation,conclusion)
    }

    @Test
    fun twoRulesWithSameConclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion("First conc")
        val rule0 = Rule(null, conclusion, emptySet())
        val rule1 = Rule(null, conclusion, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        checkSingleConclusion(interpretation, conclusion)
    }

    @Test
    fun multipleRules() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val c0 = Conclusion("First conc")
        val rule0 = Rule(null, c0, emptySet())
        val c1 = Conclusion("Second conc")
        val rule1 = Rule(null, c1, emptySet())
        val c2 = Conclusion("Third conc")
        val rule2 = Rule(null, c2, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.conclusions().size shouldBe  3
        interpretation.conclusions() shouldContain c0
        interpretation.conclusions() shouldContain c1
        interpretation.conclusions() shouldContain c2
    }

    @Test
    fun rules_giving_conclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val concA = Conclusion("A")
        val concB = Conclusion("B")
        val rule0 = Rule(null, concA, emptySet())
        val rule1 = Rule(null, concA, emptySet())
        val rule2 = Rule(null, concB, emptySet())
        interpretation.rulesGivingConclusion(concA) shouldBe setOf()

        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.rulesGivingConclusion(concA) shouldBe  setOf(rule0, rule1)
        interpretation.rulesGivingConclusion(concB) shouldBe setOf(rule2)
    }

    @Test
    fun jsonSerialisation() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val sd = serializeDeserialize(interpretation)
        assertEquals(sd, interpretation)
    }

    private fun serializeDeserialize(interpretation: Interpretation): Interpretation {
        val serialized = Json.encodeToString(interpretation)
        return Json.decodeFromString(serialized)
    }

    private fun checkSingleConclusion(interpretation: Interpretation, conclusion: Conclusion) {
        interpretation.conclusions().size shouldBe 1
        interpretation.conclusions() shouldContain conclusion
    }
}