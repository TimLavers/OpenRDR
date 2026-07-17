package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.containsText
import io.rippledown.utils.defaultDate
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

internal class RuleActionTest {
    private val notes = Attribute(1, "Notes")
    private val glucose = Attribute(2, "Glucose")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val diabetic = AssignValue(diabetesStatus, Literal("diabetic"))
    private val conclusion = Conclusion(1, "Diabetic diet advice given.")

    @Test
    fun `a value can only be assigned to a KB-assigned attribute`() {
        // When an assignment to an external attribute is constructed
        // Then it is rejected
        shouldThrow<IllegalArgumentException> {
            AssignValue(glucose, Literal("diabetic"))
        }.message shouldBe "Values can only be assigned to KB-assigned attributes, but Glucose is EXTERNAL."
    }

    @Test
    fun asText() {
        diabetic.asText() shouldBe "Diabetes status = \"diabetic\""
    }

    @Test
    fun serialization() {
        // Given actions of each kind
        val give: RuleAction = GiveConclusion(conclusion)
        val assign: RuleAction = diabetic

        // When they are serialized and deserialized
        // Then they are unchanged
        serializeDeserialize(give) shouldBe give
        serializeDeserialize(assign) shouldBe assign
    }

    @Test
    fun `a rule cannot both give a conclusion and assign a value`() {
        // When a rule is constructed with both a conclusion and an assignment
        // Then it is rejected
        shouldThrow<IllegalArgumentException> {
            Rule(1, null, conclusion, emptySet(), mutableSetOf(), diabetic)
        }.message shouldBe "A rule cannot both give a conclusion and assign a value."
    }

    @Test
    fun `the action of a rule is its assignment, its conclusion, or nothing`() {
        // Given rules with an assignment, a conclusion, and neither
        val assigning = Rule(1, null, null, emptySet(), mutableSetOf(), diabetic)
        val concluding = Rule(2, null, conclusion)
        val stopping = Rule(3, null, null)

        // Then the actions are as expected
        assigning.action shouldBe diabetic
        concluding.action shouldBe GiveConclusion(conclusion)
        stopping.action.shouldBeNull()
    }

    @Test
    fun `a fired assignment rule contributes its assignment to the interpretation`() {
        // Given an assignment rule whose condition holds for a case
        val rule = Rule(1, null, null, setOf(containsText(1, notes, "diab")), mutableSetOf(), diabetic)
        val case = with(RDRCaseBuilder()) {
            addValue(notes, defaultDate, "diabetes suspected")
            build("Fermi")
        }
        val interpretation = Interpretation(CaseId("Fermi"))

        // When the rule is applied
        rule.apply(case, interpretation)

        // Then the interpretation carries the assignment
        interpretation.assignments() shouldBe setOf(diabetic)
        interpretation.idsOfRulesAssigning(diabetesStatus) shouldBe setOf(1)
        interpretation.conclusions() shouldBe emptySet()
    }

    @Test
    fun `a child rule with no action retracts its parent's assignment`() {
        // Given an assignment rule with a stopping child whose condition holds
        val rule = Rule(1, null, null, setOf(containsText(1, notes, "diab")), mutableSetOf(), diabetic)
        val stopper = Rule(2, null, null, setOf(containsText(2, notes, "resolved")))
        rule.addChild(stopper)
        val case = with(RDRCaseBuilder()) {
            addValue(notes, defaultDate, "diabetes resolved")
            build("Fermi")
        }
        val interpretation = Interpretation(CaseId("Fermi"))

        // When the rule is applied
        rule.apply(case, interpretation)

        // Then no assignment is made
        interpretation.assignments() shouldBe emptySet()
    }

    @Test
    fun `action summaries for assignment rules`() {
        // Given an assignment rule, a retraction child and a replacement child
        val root = Rule(0)
        val assigning = Rule(1, null, null, emptySet(), mutableSetOf(), diabetic)
        root.addChild(assigning)
        val retracting = Rule(2, null, null)
        assigning.addChild(retracting)
        val replacement = AssignValue(diabetesStatus, Literal("pre-diabetic"))
        val replacing = Rule(3, null, null, emptySet(), mutableSetOf(), replacement)
        assigning.addChild(replacing)

        // Then the action summaries describe the assignments
        assigning.actionSummary() shouldBe "$RULE_TO_ASSIGN_VALUE\nDiabetes status = \"diabetic\""
        retracting.actionSummary() shouldBe "$RULE_TO_RETRACT_ASSIGNMENT\nDiabetes status = \"diabetic\""
        replacing.actionSummary() shouldBe
                "$RULE_TO_REPLACE_ASSIGNMENT\nDiabetes status = \"diabetic\"\n$WITH\nDiabetes status = \"pre-diabetic\""
    }

    @Test
    fun `rule copy preserves the assignment`() {
        // Given an assignment rule
        val rule = Rule(1, null, null, emptySet(), mutableSetOf(), diabetic)

        // When it is copied
        val copy = rule.copy()

        // Then the assignment is preserved
        copy.assignment shouldBe diabetic
        copy.structurallyEqual(rule) shouldBe true
    }

    @Test
    fun `rule summary serialization carries the assignment`() {
        // Given the summary of an assignment rule
        val summary = Rule(1, null, null, emptySet(), mutableSetOf(), diabetic).summary()

        // When it is serialized and deserialized
        val recovered = serializeDeserialize(summary)

        // Then the assignment is unchanged
        recovered.assignment shouldBe diabetic
    }
}
