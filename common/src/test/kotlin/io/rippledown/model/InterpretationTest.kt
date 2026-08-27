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
    private val wave = Attribute(1, "Wave")
    private val sun = Attribute(2, "Sun")
    private val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
    private val c2 = Attribute(11, "C2", AttributeKind.COMMENT)
    private val c3 = Attribute(12, "C3", AttributeKind.COMMENT)

    private fun comment(attribute: Attribute, text: String, vararg variables: Attribute) =
        AssignValue(attribute, CommentTemplate(text, variables.toList()))

    @Test
    fun construction() {
        val interpretation = Interpretation(caseId)
        assertEquals(interpretation.caseId, caseId)
    }

    @Test
    fun testEmpty() {
        Interpretation(caseId).assignments().size shouldBe 0
    }

    @Test
    fun singleRule() {
        val interpretation = Interpretation(caseId)
        val assignment = comment(c1, "First comment")
        val rule = Rule(0, null, emptySet(), mutableSetOf(), assignment)
        interpretation.add(rule)
        checkSingleAssignment(interpretation, assignment)
    }

    @Test
    fun twoRulesWithSameAssignment() {
        val interpretation = Interpretation(caseId)
        val assignment = comment(c1, "First comment")
        val rule0 = Rule(0, null, emptySet(), mutableSetOf(), assignment)
        val rule1 = Rule(1, null, emptySet(), mutableSetOf(), assignment)
        interpretation.add(rule0)
        interpretation.add(rule1)
        checkSingleAssignment(interpretation, assignment)
    }

    @Test
    fun multipleRules() {
        val interpretation = Interpretation(caseId)
        val a0 = comment(c1, "First comment")
        val a1 = comment(c2, "Second comment")
        val a2 = comment(c3, "Third comment")
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), a0))
        interpretation.add(Rule(1, null, emptySet(), mutableSetOf(), a1))
        interpretation.add(Rule(2, null, emptySet(), mutableSetOf(), a2))
        interpretation.assignments().size shouldBe 3
        interpretation.assignments() shouldContain a0
        interpretation.assignments() shouldContain a1
        interpretation.assignments() shouldContain a2
    }

    @Test
    fun idsOfRulesMakingAssignment() {
        val interpretation = Interpretation(caseId)
        val assignmentA = comment(c1, "A")
        val assignmentB = comment(c2, "B")
        val rule0 = Rule(0, null, emptySet(), mutableSetOf(), assignmentA)
        val rule1 = Rule(1, null, emptySet(), mutableSetOf(), assignmentA)
        val rule2 = Rule(2, null, emptySet(), mutableSetOf(), assignmentB)
        interpretation.idsOfRulesMakingAssignment(assignmentA) shouldBe setOf()

        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.idsOfRulesMakingAssignment(assignmentA) shouldBe setOf(rule0.id, rule1.id)
        interpretation.idsOfRulesMakingAssignment(assignmentB) shouldBe setOf(rule2.id)
    }

    @Test
    fun idsOfRulesAssigningAnAttribute() {
        val interpretation = Interpretation(caseId)
        val rule0 = Rule(0, null, emptySet(), mutableSetOf(), comment(c1, "A"))
        val rule1 = Rule(1, null, emptySet(), mutableSetOf(), comment(c2, "B"))
        interpretation.add(rule0)
        interpretation.add(rule1)

        interpretation.idsOfRulesAssigning(c1) shouldBe setOf(rule0.id)
        interpretation.idsOfRulesAssigning(c2) shouldBe setOf(rule1.id)
    }

    @Test
    fun addRuleSummary() {
        val interpretation = Interpretation(caseId)
        val a0 = comment(c1, "First comment")
        val a1 = comment(c2, "Second comment")
        val a2 = comment(c3, "Third comment")
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), a0).summary())
        interpretation.add(Rule(1, null, emptySet(), mutableSetOf(), a1).summary())
        interpretation.add(Rule(2, null, emptySet(), mutableSetOf(), a2).summary())
        interpretation.assignments().size shouldBe 3
        interpretation.assignments() shouldContain a0
        interpretation.assignments() shouldContain a1
        interpretation.assignments() shouldContain a2
    }

    @Test
    fun serialisationWithRule() {
        val assignment = comment(c1, "First comment")
        val conditions = setOf(
            isCondition(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conditions, mutableSetOf(), assignment)
        val interpretation = Interpretation(caseId).apply { add(rule) }
        val restored = serializeDeserialize(interpretation)
        restored.assignments() shouldBe setOf(assignment)
    }

    @Test
    fun serialisationWithRuleSummary() {
        val assignment = comment(c1, "First comment")
        val conditions = setOf(
            isCondition(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conditions, mutableSetOf(), assignment)
        val interpretation = Interpretation(caseId).apply { add(rule.summary()) }
        val restored = serializeDeserialize(interpretation)
        restored.assignments() shouldBe setOf(assignment)
    }

    @Test
    fun toCommentsShouldConvertInternalPlaceholdersToAttributeNameFormat() {
        val interpretation = Interpretation(caseId)
        val template = "The wave quality is " + VARIABLE_TOKEN + " and the air temperature is " + VARIABLE_TOKEN
        interpretation.add(
            Rule(0, null, emptySet(), mutableSetOf(), comment(c1, template, wave, sun))
        )

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
        // The stored comment carries a name the attribute no longer has.
        val staleSun = Attribute(sun.id, "Sunshine")
        val template = "The wave is " + VARIABLE_TOKEN + " and the sun is " + VARIABLE_TOKEN
        interpretation.add(
            Rule(0, null, emptySet(), mutableSetOf(), comment(c1, template, wave, staleSun))
        )

        // The current case has no value for Sun, so Sun is absent from case.attributes.
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        // The resolver knows about all knowledge base attributes, including Sun.
        val attributeById = { id: Int -> listOf(wave, sun).find { it.id == id } }

        val comments = Json.decodeFromString<Set<String>>(interpretation.toComments(case, attributeById))
        // Sun should resolve to its current name.
        comments shouldBe setOf("The wave is {Wave} and the sun is {Sun}")
    }

    @Test
    fun toCommentsShouldHandlePlainCommentsWithoutVariables() {
        val interpretation = Interpretation(caseId)
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), comment(c1, "Plain comment")))

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
    fun toCommentsShouldIncludeCommentAttributeAssignments() {
        // Given an interpretation with a plain comment assignment and a template comment assignment
        val interpretation = Interpretation(caseId)
        interpretation.add(
            RuleSummary(id = 1, assignment = comment(c1, "Plain comment."))
        )
        interpretation.add(
            RuleSummary(id = 2, assignment = comment(c2, "Wave is " + VARIABLE_TOKEN, wave))
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
        val rule = Rule(0, null, conditions, mutableSetOf(), assignment)
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
        val parentRule = Rule(0, null, parentConditions, mutableSetOf(), parentAssignment)

        val childConditions = setOf(isCondition(attributeId++, weight, "80"))
        val childAssignment = AssignValue(beta, Literal("no"))
        val childRule = Rule(1, parentRule, childConditions, mutableSetOf(), childAssignment)

        val interpretation = Interpretation(caseId).apply { add(childRule) }

        // When asking for the conditions of the child assignment
        val result = interpretation.conditionsForAssignment(childAssignment)

        // Then parent conditions come first, then child conditions
        result shouldBe listOf("Glucose contains \"12.0\"", "weight is \"80\"")
    }

    @Test
    fun `conditionsForAssignment lists the conditions of the leaf rule in alphabetical order`() {
        val interpretation = Interpretation(caseId)
        val assignment = comment(c1, "First comment")
        val conditions = setOf(
            containsText(Attribute(26, "z"), "text z"),
            containsText(Attribute(1, "A"), "text A"),
            containsText(Attribute(25, "Y"), "text Y"),
            containsText(Attribute(2, "b"), "text b"),
        )
        interpretation.add(Rule(0, null, conditions, mutableSetOf(), assignment))

        interpretation.conditionsForAssignment(assignment) shouldBe listOf(
            "A contains \"text A\"",
            "b contains \"text b\"",
            "Y contains \"text Y\"",
            "z contains \"text z\""
        )
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

    private fun checkSingleAssignment(interpretation: Interpretation, assignment: AssignValue) {
        interpretation.assignments().size shouldBe 1
        interpretation.assignments() shouldContain assignment
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
    fun `commentTexts returns plain comment text`() {
        val interpretation = Interpretation(caseId)
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), comment(c1, "Normal glucose results.")))
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("Normal glucose results.")
    }

    @Test
    fun `commentTexts returns comment text with variables in attributeName format`() {
        val interpretation = Interpretation(caseId)
        val template = "The wave quality is " + VARIABLE_TOKEN + " and the air temperature is " + VARIABLE_TOKEN
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), comment(c1, template, wave, sun)))
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
            addValue(sun, 0, "hot")
        }.build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("The wave quality is {Wave} and the air temperature is {Sun}")
    }

    @Test
    fun `commentTexts resolves variable names via attributeById when absent from case`() {
        val interpretation = Interpretation(caseId)
        val staleSun = Attribute(sun.id, "Sunshine")
        val template = "The wave is " + VARIABLE_TOKEN + " and the sun is " + VARIABLE_TOKEN
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), comment(c1, template, wave, staleSun)))
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)
        val attributeById = { id: Int -> listOf(wave, sun).find { it.id == id } }

        interpretation.commentTexts(case, attributeById) shouldBe setOf("The wave is {Wave} and the sun is {Sun}")
    }

    @Test
    fun `commentTexts returns Literal assignment value`() {
        val interpretation = Interpretation(caseId)
        interpretation.add(
            RuleSummary(id = 1, assignment = AssignValue(c1, Literal("A literal comment.")))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("A literal comment.")
    }

    @Test
    fun `commentTexts omits unresolved ByDefinition comment assignments`() {
        val interpretation = Interpretation(caseId)
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
    fun `commentTexts returns multiple comment assignments sorted by attribute id`() {
        val interpretation = Interpretation(caseId)
        interpretation.add(
            RuleSummary(id = 1, assignment = comment(c2, "Second comment"))
        )
        interpretation.add(
            RuleSummary(id = 2, assignment = comment(c1, "First comment"))
        )
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("First comment", "Second comment")
    }

    @Test
    fun `commentTexts deduplicates identical texts given by different attributes`() {
        val interpretation = Interpretation(caseId)
        interpretation.add(RuleSummary(id = 1, assignment = comment(c1, "Same text")))
        interpretation.add(RuleSummary(id = 2, assignment = comment(c2, "Same text")))
        val case = RDRCaseBuilder().build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("Same text")
    }

    @Test
    fun `commentTexts handles a mixture of plain and templated comments`() {
        val interpretation = Interpretation(caseId)
        interpretation.add(Rule(0, null, emptySet(), mutableSetOf(), comment(c1, "First comment")))
        interpretation.add(
            Rule(1, null, emptySet(), mutableSetOf(), comment(c2, "Wave is " + VARIABLE_TOKEN, wave))
        )
        interpretation.add(RuleSummary(id = 3, assignment = comment(c3, "Third comment")))
        val case = RDRCaseBuilder().apply {
            addValue(wave, 0, "excellent")
        }.build("Test", 1)

        interpretation.commentTexts(case) shouldBe setOf("First comment", "Wave is {Wave}", "Third comment")
    }

    @Test
    fun `resolveDefinitions replaces by-definition assignments with the stored definition`() {
        // Given an interpretation holding a by-definition comment assignment
        val interpretation = Interpretation(caseId)
        interpretation.add(RuleSummary(id = 1, assignment = AssignValue(c1, ByDefinition)))
        val definition = CommentTemplate("Given by the definition.")

        // When the definitions are resolved
        interpretation.resolveDefinitions { if (it == c1) definition else null }

        // Then the assignment carries the definition
        interpretation.assignments() shouldBe setOf(AssignValue(c1, definition))
        interpretation.commentTexts(RDRCaseBuilder().build("Test", 1)) shouldBe setOf("Given by the definition.")
    }
}
