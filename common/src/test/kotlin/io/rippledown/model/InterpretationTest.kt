package io.rippledown.model

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.isCondition
import io.rippledown.model.rule.*
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

    @Test
    fun toCommentsShouldIncludeCommentAttributeAssignments() {
        // Given an interpretation with a literal comment assignment and a template comment assignment
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        val c2 = Attribute(11, "C2", AttributeKind.COMMENT)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(c1, CommentTemplate("Plain comment.")))
        )
        interpretation.add(
            RuleSummary(
                id = 2,
                assignment = AssignValue(c2, CommentTemplate("Wave is " + VARIABLE_TOKEN, listOf(wave)))
            )
        )
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        // When the comments are produced for the LLM
        val comments = Json.decodeFromString<Set<String>>(interpretation.toComments(case))

        // Then both comments appear, with template variables in {attributeName} format
        comments shouldBe setOf("Plain comment.", "Wave is {Wave}")
    }

    @Test
    fun toCommentsShouldOmitUnresolvedAndNonCommentAssignments() {
        // Given an unresolved ByDefinition comment assignment and a derived-value assignment
        val interpretation = Interpretation(caseId)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        val bmi = Attribute(11, "BMI", AttributeKind.DERIVED)
        interpretation.add(RuleSummary(id = 1, assignment = AssignValue(c1, ByDefinition)))
        interpretation.add(RuleSummary(id = 2, assignment = AssignValue(bmi, Literal("25"))))
        val case = RDRCaseBuilder().build("Test", 1)

        // When the comments are produced for the LLM
        val comments = Json.decodeFromString<Set<String>>(interpretation.toComments(case))

        // Then neither assignment contributes a comment
        comments shouldBe emptySet()
    }

    @Test
    fun `conditionsForAssignment returns condition texts for a rule that assigned a value`() {
        // Given an interpretation with a rule that assigns a derived value
        val glucose = Attribute(attributeId++, "Glucose", AttributeKind.EXTERNAL)
        val weight = Attribute(attributeId++, "weight", AttributeKind.EXTERNAL)
        val diabetesStatus = Attribute(attributeId++, "Diabetes status", AttributeKind.DERIVED)
        val assignment = AssignValue(diabetesStatus, Literal("diabetic"))
        val conditions = setOf(
            containsText(glucose, "12.0"),
            isCondition(attributeId++, weight, "80")
        )
        val rule = Rule(0, null, null, conditions, assignment = assignment)
        val interpretation = Interpretation(caseId).apply { add(rule) }

        // When asking for the conditions of that assignment
        val result = interpretation.conditionsForAssignment(assignment)

        // Then the condition texts from root are returned
        result shouldContain "Glucose contains \"12.0\""
    }

    @Test
    fun `conditionsForAssignment returns empty list when no rule assigned the value`() {
        // Given an interpretation with no assignment rules
        val interpretation = Interpretation(caseId)
        val assignment = AssignValue(
            Attribute(0, "BMI", AttributeKind.DERIVED),
            Literal("25")
        )

        // When asking for conditions of a non-existent assignment
        val result = interpretation.conditionsForAssignment(assignment)

        // Then an empty list is returned
        result.shouldBeEmpty()
    }

    @Test
    fun `conditionsForAssignment lists parent conditions first for chained rules`() {
        // Given a parent rule with conditions and a child rule with its own conditions
        val glucose = Attribute(attributeId++, "Glucose", AttributeKind.EXTERNAL)
        val weight = Attribute(attributeId++, "weight", AttributeKind.EXTERNAL)
        val alpha = Attribute(attributeId++, "Alpha", AttributeKind.DERIVED)
        val beta = Attribute(attributeId++, "Beta", AttributeKind.DERIVED)
        val parentConditions = setOf(containsText(glucose, "12.0"))
        val parentAssignment = AssignValue(alpha, Literal("yes"))
        val parentRule = Rule(0, null, null, parentConditions, assignment = parentAssignment)

        val childConditions = setOf(isCondition(attributeId++, weight, "80"))
        val childAssignment = AssignValue(beta, Literal("no"))
        val childRule = Rule(1, parentRule, null, childConditions, assignment = childAssignment)

        val interpretation = Interpretation(caseId).apply { add(childRule) }

        // When asking for the conditions of the child assignment
        val result = interpretation.conditionsForAssignment(childAssignment)

        // Then parent conditions come first, then child conditions
        result shouldBe listOf("Glucose contains \"12.0\"", "weight is \"80\"")
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

    // -----------------------------------------------------------------
    // commentTexts()
    // -----------------------------------------------------------------

    @Test
    fun `commentTexts returns empty set for empty interpretation`() {
        val interpretation = Interpretation(caseId)
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe emptySet()
    }

    @Test
    fun `commentTexts returns plain conclusion text`() {
        val interpretation = Interpretation(caseId)
        val conclusion = Conclusion(1, "Normal glucose results.")
        interpretation.add(Rule(0, null, conclusion, emptySet()))
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("Normal glucose results.")
    }

    @Test
    fun `commentTexts returns conclusion text with variables in attributeName format`() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val sun = Attribute(2, "Sun")
        val template = "The wave quality is " + VARIABLE_TOKEN + " and the air temperature is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(wave.id), CommentVariable(sun.id))
        val conclusion = Conclusion(1, template, variables)
        interpretation.add(Rule(0, null, conclusion, emptySet()))
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
            addValue(sun, 0, "hot")
        }.build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("The wave quality is {Wave} and the air temperature is {Sun}")
    }

    @Test
    fun `commentTexts resolves variable names via attributeById when absent from case`() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val sun = Attribute(2, "Sun")
        val template = "The wave is " + VARIABLE_TOKEN + " and the sun is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(wave.id), CommentVariable(sun.id))
        val conclusion = Conclusion(1, template, variables)
        interpretation.add(Rule(0, null, conclusion, emptySet()))
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)
        val attributeById = { id: Int -> listOf(wave, sun).find { it.id == id } }

        interpretation.commentTexts(case, attributeById) shouldBe setOf("The wave is {Wave} and the sun is {Sun}")
    }

    @Test
    fun `commentTexts falls back to unknown when attribute cannot be resolved`() {
        val interpretation = Interpretation(caseId)
        val template = "The sun is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(99))
        val conclusion = Conclusion(1, template, variables)
        interpretation.add(Rule(0, null, conclusion, emptySet()))
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("The sun is {unknown}")
    }

    @Test
    fun `commentTexts returns CommentTemplate assignment text`() {
        val interpretation = Interpretation(caseId)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(c1, CommentTemplate("Plain comment.")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("Plain comment.")
    }

    @Test
    fun `commentTexts returns CommentTemplate assignment with variables in attributeName format`() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(
            RuleSummary(
                id = 1,
                assignment = AssignValue(c1, CommentTemplate("Wave is " + VARIABLE_TOKEN, listOf(wave)))
            )
        )
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("Wave is {Wave}")
    }

    @Test
    fun `commentTexts returns Literal assignment value`() {
        val interpretation = Interpretation(caseId)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(c1, Literal("A literal comment.")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("A literal comment.")
    }

    @Test
    fun `commentTexts omits unresolved ByDefinition comment assignments`() {
        val interpretation = Interpretation(caseId)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(c1, ByDefinition))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe emptySet()
    }

    @Test
    fun `commentTexts excludes non-comment assignments`() {
        val interpretation = Interpretation(caseId)
        val bmi = Attribute(11, "BMI", AttributeKind.DERIVED)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(bmi, Literal("25")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe emptySet()
    }

    @Test
    fun `commentTexts returns both conclusion texts and comment assignment texts`() {
        val interpretation = Interpretation(caseId)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(Rule(0, null, Conclusion(1, "From conclusion"), emptySet()))
        interpretation.add(
            RuleSummary(id = 2, assignment = AssignValue(c1, CommentTemplate("From assignment")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("From conclusion", "From assignment")
    }

    @Test
    fun `commentTexts returns multiple comment assignments sorted by attribute id`() {
        val interpretation = Interpretation(caseId)
        val c2 = Attribute(20, "C2", AttributeKind.COMMENT)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(c2, CommentTemplate("Second comment")))
        )
        interpretation.add(
            RuleSummary(id = 2, assignment = AssignValue(c1, CommentTemplate("First comment")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("First comment", "Second comment")
    }

    @Test
    fun `commentTexts deduplicates identical texts from conclusion and assignment`() {
        val interpretation = Interpretation(caseId)
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        interpretation.add(Rule(0, null, Conclusion(1, "Same text"), emptySet()))
        interpretation.add(
            RuleSummary(id = 2, assignment = AssignValue(c1, CommentTemplate("Same text")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("Same text")
    }

    @Test
    fun `commentTexts handles mixed conclusions with variables and comment assignments`() {
        val interpretation = Interpretation(caseId)
        val wave = Attribute(1, "Wave")
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        val conclusion = Conclusion(1, "First comment")
        val template = "Wave is " + VARIABLE_TOKEN
        val variables = listOf(CommentVariable(wave.id))
        val conclusion2 = Conclusion(2, template, variables)
        interpretation.add(Rule(0, null, conclusion, emptySet()))
        interpretation.add(Rule(1, null, conclusion2, emptySet()))
        interpretation.add(
            RuleSummary(id = 3, assignment = AssignValue(c1, CommentTemplate("Third comment")))
        )
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("First comment", "Wave is {Wave}", "Third comment")
    }
}