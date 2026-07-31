package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.*
import io.rippledown.model.rule.*
import kotlin.test.Test

internal class WithResolvedDefinitionsTest {
    private val glucose = Attribute(1, "Glucose")
    private val bmi = Attribute(10, "BMI", AttributeKind.DERIVED)
    private val riskScore = Attribute(11, "Risk score", AttributeKind.DERIVED)
    private val caseId = CaseId(42, "Bragg")
    private val bmiDefinition = Formula(AttributeValue(glucose))
    private val resolver: DefinitionResolver = { attribute -> if (attribute == bmi) bmiDefinition else null }

    private fun interpretation(vararg summaries: RuleSummary) = Interpretation(caseId).apply {
        summaries.forEach { add(it) }
    }

    @Test
    fun `a by-definition assignment is replaced by the stored definition`() {
        // Given an interpretation with a by-definition assignment
        val original = interpretation(RuleSummary(id = 7, assignment = AssignValue(bmi, ByDefinition)))

        // When the definitions are resolved
        val resolved = original.withResolvedDefinitions(resolver)

        // Then the assignment carries the definition
        resolved.assignments() shouldBe setOf(AssignValue(bmi, bmiDefinition))
    }

    @Test
    fun `a concrete assignment is left unchanged`() {
        // Given summaries with concrete literal and formula assignments
        val literalSummary = RuleSummary(id = 1, assignment = AssignValue(riskScore, Literal("7")))
        val formulaSummary = RuleSummary(id = 2, assignment = AssignValue(bmi, Formula(AttributeValue(glucose))))
        val original = interpretation(literalSummary, formulaSummary)

        // When the definitions are resolved
        val resolved = original.withResolvedDefinitions(resolver)

        // Then the summaries are carried over as the same instances
        resolved.ruleSummaries.first { it.id == 1 } shouldBeSameInstanceAs literalSummary
        resolved.ruleSummaries.first { it.id == 2 } shouldBeSameInstanceAs formulaSummary
    }

    @Test
    fun `a by-definition assignment with no stored definition is left as is`() {
        // Given a by-definition assignment for an attribute with no definition
        val summary = RuleSummary(id = 7, assignment = AssignValue(riskScore, ByDefinition))
        val original = interpretation(summary)

        // When the definitions are resolved with a resolver that has no definition for it
        val resolved = original.withResolvedDefinitions(resolver)

        // Then the summary is carried over unchanged
        resolved.ruleSummaries.single() shouldBeSameInstanceAs summary
        original.withResolvedDefinitions(NO_DEFINITIONS).ruleSummaries.single() shouldBeSameInstanceAs summary
    }

    @Test
    fun `a conclusion-only summary is carried over unchanged`() {
        // Given a summary with a conclusion and no assignment
        val summary = RuleSummary(id = 3, conclusion = Conclusion(1, "Advice given."))
        val original = interpretation(summary)

        // When the definitions are resolved
        val resolved = original.withResolvedDefinitions(resolver)

        // Then the summary is carried over as the same instance
        resolved.ruleSummaries.single() shouldBeSameInstanceAs summary
    }

    @Test
    fun `the other fields of a resolved summary are preserved`() {
        // Given a by-definition summary with id and condition texts
        val original = interpretation(
            RuleSummary(
                id = 7,
                conditionTextsFromRoot = listOf("Glucose ≥ 11.0"),
                assignment = AssignValue(bmi, ByDefinition)
            )
        )

        // When the definitions are resolved
        val resolvedSummary = original.withResolvedDefinitions(resolver).ruleSummaries.single()

        // Then only the assignment expression has changed
        resolvedSummary.id shouldBe 7
        resolvedSummary.conditionTextsFromRoot shouldBe listOf("Glucose ≥ 11.0")
        resolvedSummary.assignment shouldBe AssignValue(bmi, bmiDefinition)

        // And the conditions for the resolved assignment can be looked up
        original.withResolvedDefinitions(resolver)
            .conditionsForAssignment(AssignValue(bmi, bmiDefinition)) shouldBe listOf("Glucose ≥ 11.0")
    }

    @Test
    fun `the case id is preserved`() {
        interpretation().withResolvedDefinitions(resolver).caseId shouldBe caseId
    }

    @Test
    fun `the original interpretation is not mutated`() {
        // Given an interpretation with a by-definition assignment
        val original = interpretation(RuleSummary(id = 7, assignment = AssignValue(bmi, ByDefinition)))

        // When the definitions are resolved
        original.withResolvedDefinitions(resolver)

        // Then the original still carries the sentinel
        original.assignments() shouldBe setOf(AssignValue(bmi, ByDefinition))
    }

    @Test
    fun `a mixture of summaries is resolved summary by summary`() {
        // Given by-definition, concrete, and conclusion-only summaries
        val original = interpretation(
            RuleSummary(id = 1, assignment = AssignValue(bmi, ByDefinition)),
            RuleSummary(id = 2, assignment = AssignValue(riskScore, Literal("7"))),
            RuleSummary(id = 3, conclusion = Conclusion(1, "Advice given."))
        )

        // When the definitions are resolved
        val resolved = original.withResolvedDefinitions(resolver)

        // Then only the by-definition assignment has changed
        resolved.ruleSummaries.size shouldBe 3
        resolved.assignments() shouldBe setOf(
            AssignValue(bmi, bmiDefinition),
            AssignValue(riskScore, Literal("7"))
        )
        resolved.conclusions() shouldBe setOf(Conclusion(1, "Advice given."))
    }
}
