package io.rippledown.model

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.isCondition
import io.rippledown.model.rule.Rule
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class InterpretationTest {
    private val caseId = CaseId(1234, "Case 1")
    private var attributeId = 0
    private var conditionId = 0

    @Test
    fun construction() {
        val interpretation = Interpretation(caseId)
        assertEquals(interpretation.caseId, caseId)
    }

    @Test
    fun testEmpty() {
        Interpretation(caseId).conclusions().size shouldBe 0
    }

    @Test
    fun singleRule() {
        val interpretation = Interpretation(caseId)
        val conclusion = Conclusion(2, "First conclusion")
        val rule = Rule(0, null, conclusion, emptySet())
        interpretation.add(rule)
        checkSingleConclusion(interpretation, conclusion)
    }

    @Test
    fun twoRulesWithSameConclusion() {
        val interpretation = Interpretation(caseId)
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, conclusion, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        checkSingleConclusion(interpretation, conclusion)
    }

    @Test
    fun multipleRules() {
        val interpretation = Interpretation(caseId)
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
        val interpretation = Interpretation(caseId)
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
        val interpretation = Interpretation(caseId)
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
    fun serialisationWithRule() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            isCondition(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conclusion, conditions)
        val interpretation = Interpretation(caseId).apply { add(rule) }
        val restored = serializeDeserialize(interpretation)
        restored.conclusions() shouldBe setOf(conclusion)
    }

    @Test
    fun serialisationWithRuleSummary() {
        val conclusion = Conclusion(1,"First conc")
        val conditions = setOf(
            isCondition(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conclusion, conditions)
        val ruleSummary = rule.summary()
        val interpretation = Interpretation(caseId).apply { add(ruleSummary) }
        val restored = serializeDeserialize(interpretation)
        restored.conclusions() shouldBe setOf(conclusion)
    }

    @Test
    fun shouldReturnConditionsForConclusion() {
        val interpretation = Interpretation(caseId)
        val c0 = Conclusion(1, "First conc")
        val conditions0 = setOf(containsText(Attribute(attributeId++, "A"), "text A"), containsText(Attribute(
            attributeId++,
            "B"
        ), "text B"))
        val rule0 = Rule(0, null, c0, conditions0)
        val c1 = Conclusion(2, "Second conc")
        val conditions1 = setOf(containsText(Attribute(attributeId++, "C"), "text C"), containsText(Attribute(
            attributeId++,
            "D"
        ), "text D"))
        val rule1 = Rule(1, null, c1, conditions1)
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.conditionsForConclusion(c0) shouldBe listOf("A contains \"text A\"", "B contains \"text B\"")
        interpretation.conditionsForConclusion(c1) shouldBe listOf("C contains \"text C\"", "D contains \"text D\"")
    }

    @Test
    fun conditionsForConclusionShouldBeInAlphaOrderForTheLeafRule() {
        val interpretation = Interpretation(caseId)
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            containsText(Attribute(attributeId++, "z"), "text z"),
            containsText(Attribute(attributeId++, "A"), "text A"),
            containsText(Attribute(attributeId++, "Y"), "text Y"),
            containsText(Attribute(attributeId++, "b"), "text b"),
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
    fun conditionsForConclusionShouldListConditionsOfParentRulesFirst() {
        val interpretation = Interpretation(caseId)
        val conclusion0 = Conclusion(1, "First conc")
        val conclusion1 = Conclusion(2, "Second conc")
        val conditions0 = setOf(
            containsText(Attribute(26, "z"), "text z"),
            containsText(Attribute(1, "A"), "text A"),
            containsText(Attribute(25, "Y"), "text Y"),
            containsText(Attribute(2, "b"), "text b"),
        )
        val conditions1 = setOf(
            containsText(Attribute(18, "r"), "text r"),
            containsText(Attribute(19, "s"), "text s"),
            containsText(Attribute(16, "p"), "text p"),
            containsText(Attribute(17, "q"), "text q"),
        )
        val rule0 = Rule(0, null, conclusion0, conditions0)
        val rule1 = Rule(1, rule0, conclusion1, conditions1)
        interpretation.add(rule1)
        interpretation.conditionsForConclusion(conclusion1) shouldBe listOf(
            "A contains \"text A\"",
            "b contains \"text b\"",
            "Y contains \"text Y\"",
            "z contains \"text z\"",
            "p contains \"text p\"",
            "q contains \"text q\"",
            "r contains \"text r\"",
            "s contains \"text s\""
        )
    }

    @Test
    fun toCommentsShouldConvertInternalPlaceholdersToAttributeNameFormat() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val sun = Attribute(2, "Sun")
        val template = "The wave quality is " + VARIABLE_TOKEN + " and the air temperature is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(wave.id), CommentVariable(sun.id))
        val conclusion = Conclusion(1, template, variables)
        val rule = Rule(0, null, conclusion, emptySet())
        interpretation.add(rule)

        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
            addValue(sun, 0, "hot")
        }.build("Test", 1)

        val commentsJson = interpretation.toComments(case)
        val comments = Json.decodeFromString<Set<String>>(commentsJson)
        // Bot should see {attributeName} format, not internal ${}
        comments shouldBe setOf("The wave quality is {Wave} and the air temperature is {Sun}")
    }

    @Test
    fun toCommentsShouldResolveAttributeNameViaResolverWhenAbsentFromCase() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val sun = Attribute(2, "Sun")
        val template = "The wave is " + VARIABLE_TOKEN + " and the sun is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(wave.id), CommentVariable(sun.id))
        val conclusion = Conclusion(1, template, variables)
        interpretation.add(Rule(0, null, conclusion, emptySet()))

        // The current case has no value for Sun, so Sun is absent from case.attributes.
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        // The resolver knows about all knowledge base attributes, including Sun.
        val attributeById = { id: Int -> listOf(wave, sun).find { it.id == id } }

        val comments = Json.decodeFromString<Set<String>>(interpretation.toComments(case, attributeById))
        // Sun should resolve to its name rather than falling back to {unknown}.
        comments shouldBe setOf("The wave is {Wave} and the sun is {Sun}")
    }

    @Test
    fun toCommentsShouldFallBackToUnknownWhenAttributeCannotBeResolved() {
        val interpretation = Interpretation(caseId)
        val template = "The sun is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(99))
        val conclusion = Conclusion(1, template, variables)
        interpretation.add(Rule(0, null, conclusion, emptySet()))

        val case = RDRCaseBuilder().build("Test", 1)

        val comments = Json.decodeFromString<Set<String>>(interpretation.toComments(case))
        comments shouldBe setOf("The sun is {unknown}")
    }

    @Test
    fun toCommentsShouldHandlePlainCommentsWithoutVariables() {
        val interpretation = Interpretation(caseId)
        val conclusion = Conclusion(1, "Plain comment")
        val rule = Rule(0, null, conclusion, emptySet())
        interpretation.add(rule)

        val case = RDRCaseBuilder().build("Test", 1)

        val commentsJson = interpretation.toComments(case)
        val comments = Json.decodeFromString<Set<String>>(commentsJson)
        comments shouldBe setOf("Plain comment")
    }

    @Test
    fun toCommentsShouldReturnEmptyArrayForEmptyInterpretation() {
        val interpretation = Interpretation(caseId)
        val case = RDRCaseBuilder().build("Test", 1)

        val commentsJson = interpretation.toComments(case)
        val comments = Json.decodeFromString<Set<String>>(commentsJson)
        comments shouldBe emptySet()
    }

    @Test
    fun toCommentsShouldConvertInternalPlaceholdersForMultipleComments() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val conclusion1 = Conclusion(1, "First comment")
        val template2 = "Wave is " + VARIABLE_TOKEN
        val variables2 = listOf(CommentVariable(wave.id))
        val conclusion2 = Conclusion(2, template2, variables2)
        val rule1 = Rule(0, null, conclusion1, emptySet())
        val rule2 = Rule(1, null, conclusion2, emptySet())
        interpretation.add(rule1)
        interpretation.add(rule2)

        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        val commentsJson = interpretation.toComments(case)
        val comments = Json.decodeFromString<Set<String>>(commentsJson)
        // Bot should see {attributeName} format
        comments shouldBe setOf("First comment", "Wave is {Wave}")
    }

    private fun containsText(attribute: Attribute, match: String): EpisodicCondition {
        return io.rippledown.model.condition.containsText(conditionId++, attribute, match)
    }

    private fun serializeDeserialize(interpretation: Interpretation): Interpretation {
        val format = Json {
            allowStructuredMapKeys = true
            prettyPrint = true
        }
        val serialized = format.encodeToString(interpretation)
        return format.decodeFromString(serialized)
    }

    private fun checkSingleConclusion(interpretation: Interpretation, conclusion: Conclusion) {
        interpretation.conclusions().size shouldBe 1
        interpretation.conclusions() shouldContain conclusion
    }
}