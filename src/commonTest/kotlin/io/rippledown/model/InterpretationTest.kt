package io.rippledown.model

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.condition.Is
import io.rippledown.model.diff.*
import io.rippledown.model.rule.Rule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InterpretationTest {
    private val caseId = CaseId("1234", "Case 1")
    private var attributeId = 0
    private var conditionId = 0

    @Test
    fun construction() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        assertEquals(interpretation.caseId, caseId)
        assertEquals(interpretation.verifiedText, "Whatever, blah.")
        assertEquals(interpretation.textGivenByRules(), "")
    }

    @Test
    fun testEmpty() {
        Interpretation(caseId, "Whatever, blah.").conclusions().size shouldBe 0
    }

    @Test
    fun textGivenByRules() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        interpretation.textGivenByRules() shouldBe ""

        val conclusion = Conclusion(1, "First conclusion")
        val rule = Rule(0, null, conclusion, emptySet())
        interpretation.add(rule)
        interpretation.textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesWithDuplicateConclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, conclusion, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesWithNullRuleConclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, null, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesHasConclusionsInABOrder() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val rule0 = Rule(0, null, Conclusion(1, "C"), emptySet())
        val rule1 = Rule(1, null, Conclusion(2, "A"), emptySet())
        val rule2 = Rule(1, null, Conclusion(3, "B"), emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.textGivenByRules() shouldBe "A B C"
    }

    @Test
    fun singleRule() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion(2, "First conclusion")
        val rule = Rule(0, null, conclusion, emptySet())
        interpretation.add(rule)
        checkSingleConclusion(interpretation, conclusion)
    }

    @Test
    fun twoRulesWithSameConclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, conclusion, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        checkSingleConclusion(interpretation, conclusion)
    }

    @Test
    fun multipleRules() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val c0 = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, c0, emptySet())
        val c1 = Conclusion(2, "Second conclusion")
        val rule1 = Rule(1, null, c1, emptySet())
        val c2 = Conclusion(3, "Third conclusion")
        val rule2 = Rule(2, null, c2, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.conclusions().size shouldBe 3
        interpretation.conclusions() shouldContain c0
        interpretation.conclusions() shouldContain c1
        interpretation.conclusions() shouldContain c2
    }

    @Test
    fun idsOfRulesGivingConclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val concA = Conclusion(1, "A")
        val concB = Conclusion(2, "B")
        val rule0 = Rule(0, null, concA, emptySet())
        val rule1 = Rule(1, null, concA, emptySet())
        val rule2 = Rule(2, null, concB, emptySet())
        interpretation.idsOfRulesGivingConclusion(concA) shouldBe setOf()

        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.idsOfRulesGivingConclusion(concA) shouldBe setOf(rule0.id, rule1.id)
        interpretation.idsOfRulesGivingConclusion(concB) shouldBe setOf(rule2.id)
    }

    @Test
    fun addRuleSummary() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val c0 = Conclusion(1, "First conc")
        val rule0 = Rule(0, null, c0, emptySet())
        val c1 = Conclusion(2, "Second conc")
        val rule1 = Rule(1, null, c1, emptySet())
        val c2 = Conclusion( 3, "Third conc")
        val rule2 = Rule(2, null, c2, emptySet())
        interpretation.add(rule0.summary())
        interpretation.add(rule1.summary())
        interpretation.add(rule2.summary())
        interpretation.conclusions().size shouldBe 3
        interpretation.conclusions() shouldContain c0
        interpretation.conclusions() shouldContain c1
        interpretation.conclusions() shouldContain c2
    }

    @Test
    fun jsonSerialisation() {
        val conclusion = Conclusion("First conc")
        val conditions = setOf(
            Is(Attribute("x"), "1"),
        )
        val verifiedText = "I can verify that is true."
        val diffList = DiffList(
            listOf(
                Addition("I can verify that is true."),
                Removal("I can verify that is false."),
                Replacement("I can verify that is false.", "I can verify that is true."),
                Unchanged("I can verify that is true or false.")
            )
        )
        val rule = Rule("r0", null, conclusion, conditions)
        val interpretation = Interpretation(
            caseId,
            verifiedText,
            diffList
        ).apply { add(rule) }
        val sd = serializeDeserialize(interpretation)
        assertEquals(sd, interpretation)
    }

    @Test
    fun jsonSerialisationWithNoVerifiedTextOrDiffList() {
        val conclusion = Conclusion("First conc")
        val conditions = setOf(
            Is(Attribute("x"), "1"),
        )
        val rule = Rule("r0", null, conclusion, conditions)
        val interpretation = Interpretation(caseId).apply { add(rule) }
        val sd = serializeDeserialize(interpretation)
        assertEquals(sd, interpretation)
    }

    @Test
    fun shouldReturnConditionsForConclusion() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val c0 = Conclusion(1, "First conc")
        val conditions0 = setOf(containsText(Attribute("A", attributeId++), "text A"), containsText(Attribute("B", attributeId++), "text B"))
        val rule0 = Rule(0, null, c0, conditions0)
        val c1 = Conclusion(2, "Second conc")
        val conditions1 = setOf(containsText(Attribute("C", attributeId++), "text C"), containsText(Attribute("D", attributeId++), "text D"))
        val rule1 = Rule(1, null, c1, conditions1)
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.conditionsForConclusion(c0) shouldBe listOf("A contains \"text A\"", "B contains \"text B\"")
        interpretation.conditionsForConclusion(c1) shouldBe listOf("C contains \"text C\"", "D contains \"text D\"")
    }

    @Test
    fun conditionsForConclusionShouldBeInAlphaOrder() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            containsText(Attribute("z", attributeId++), "text z"),
            containsText(Attribute("A", attributeId++), "text A"),
            containsText(Attribute("Y", attributeId++), "text Y"),
            containsText(Attribute("b", attributeId++), "text b"),
        )
        val rule0 = Rule(0, null, conclusion, conditions)
        interpretation.add(rule0)
        interpretation.conditionsForConclusion(conclusion) shouldBe listOf(
            "A contains \"text A\"",
            "b contains \"text b\"",
            "Y contains \"text Y\"",
            "z contains \"text z\""
        )
    }

    @Test
    fun resettingTheInterpretationShouldNotChangeTheVerifiedText() {
        val verifiedText = "I can verify that is true."
        val interpretation = Interpretation(caseId, verifiedText)
        interpretation.reset()
        interpretation.verifiedText shouldBe verifiedText
    }

    @Test
    fun resettingTheInterpretationShouldNotChangeTheDiffList() {
        val verifiedText = "I can verify that is true."
        val diffList = DiffList(
            listOf(
                Addition("I can verify that is true."),
                Removal("I can verify that is false."),
                Replacement("I can verify that is false.", "I can verify that is true."),
                Unchanged("I can verify that is true or false.")
            )
        )
        val interpretation = Interpretation(caseId, verifiedText, diffList)
        interpretation.reset()
        interpretation.diffList shouldBe diffList
    }

    @Test
    fun latestTextShouldBeTheVerifiedTextIfNotNull() {
        val verifiedText = "I can verify that is true."
        val conclusion = Conclusion("First conc")
        val conditions = setOf(
            Is(Attribute("x"), "1"),
        )
        val rule0 = Rule("r0", null, conclusion, conditions)
        with(Interpretation(caseId, verifiedText)) {
            add(rule0)
            latestText() shouldBe verifiedText
        }
    }

    @Test
    fun latestTextShouldBeTheInterpretationIfNoVerifiedText() {
        val conclusion = Conclusion("First conc")
        val conditions = setOf(
            Is(Attribute("x"), "1"),
        )
        val rule0 = Rule("r0", null, conclusion, conditions)
        with(Interpretation(caseId)) {
            add(rule0)
            latestText() shouldBe conclusion.text
        }
    }

    private fun containsText(attribute: Attribute, match: String): ContainsText {
        return ContainsText(conditionId++, attribute, match)
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