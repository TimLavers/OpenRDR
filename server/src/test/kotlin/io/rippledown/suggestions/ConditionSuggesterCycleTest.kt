package io.rippledown.suggestions

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.isPresent
import io.rippledown.model.rule.*
import io.rippledown.utils.defaultDate
import kotlin.test.Test

class ConditionSuggesterCycleTest {
    private val glucose = Attribute(1, "Glucose")
    private val riskLevel = Attribute(10, "Risk level", AttributeKind.DERIVED)
    private val diabetesStatus = Attribute(11, "Diabetes status", AttributeKind.DERIVED)

    private val ruleFactory = object : RuleFactory {
        override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>) =
            Rule(100, parent, conditions, mutableSetOf(), assignment)
    }

    /**
     * A tree in which Diabetes status is assigned when Risk level is in the
     * case, so that Diabetes status depends on Risk level.
     */
    private fun treeWithDependentRule(): RuleTree {
        val tree = RuleTree()
        val rule = ruleFactory.createRuleAndAddToParent(
            tree.root,
            AssignValue(diabetesStatus, Literal("diabetic")),
            setOf(isPresent(riskLevel, 1000))
        )
        tree.root.addChild(rule)
        return tree
    }

    /**
     * A case with values for Glucose and for the derived Diabetes status,
     * as the session case would have after materialisation.
     */
    private fun materialisedCase() = with(RDRCaseBuilder()) {
        addValue(glucose, defaultDate, "5.0")
        addValue(diabetesStatus, defaultDate, "diabetic")
        build("Case")
    }

    @Test
    fun `cycle-creating conditions are not suggested`() {
        // Given a session assigning Risk level, on which Diabetes status depends
        val ctx = SuggestionContext(
            sessionCase = materialisedCase(),
            attributes = setOf(glucose, riskLevel, diabetesStatus),
            action = ChangeTreeToAddAssignment(AssignValue(riskLevel, Literal("low"))),
            ruleTree = treeWithDependentRule(),
        )

        // When suggestions are generated
        val suggestions = ConditionSuggester(ctx).allSuggestions()

        // Then no suggestion refers to Diabetes status, but Glucose ones remain
        suggestions.any {
            "Diabetes status" in it.initialSuggestion().attributeNames()
        } shouldBe false
        suggestions.any {
            "Glucose" in it.initialSuggestion().attributeNames()
        } shouldBe true
    }

    @Test
    fun `conditions on derived attributes are suggested when they create no cycle`() {
        // Given a session adding a conclusion, not an assignment
        val ctx = SuggestionContext(
            sessionCase = materialisedCase(),
            attributes = setOf(glucose, riskLevel, diabetesStatus),
            action = null,
            ruleTree = treeWithDependentRule(),
        )

        // When suggestions are generated
        val suggestions = ConditionSuggester(ctx).allSuggestions()

        // Then conditions on the derived attribute are offered
        suggestions.any {
            "Diabetes status" in it.initialSuggestion().attributeNames()
        } shouldBe true
    }

    /**
     * Case without the derived attribute value: absence condition offered,
     * presence condition not offered.
     */
    private fun caseWithoutDerivedValue() = with(RDRCaseBuilder()) {
        addValue(glucose, defaultDate, "5.0")
        build("CaseNoDerived")
    }

    @Test
    fun `presence and absence suggestions are offered for derived attributes`() {
        // Given a session case that has the derived value
        val ctx = SuggestionContext(
            sessionCase = materialisedCase(),
            attributes = setOf(glucose, riskLevel, diabetesStatus),
            action = null,
            ruleTree = treeWithDependentRule(),
        )

        // When suggestions are generated
        val suggestions = ConditionSuggester(ctx).allSuggestions().map { it.asText() }

        // Then presence is suggested for Diabetes status and absence is not
        suggestions shouldContain "Diabetes status is in case"
        suggestions shouldNotContain "Diabetes status is not in case"

        // And for a case without the derived value, absence is suggested
        val absentCtx = ctx.copy(sessionCase = caseWithoutDerivedValue())
        val absentSuggestions = ConditionSuggester(absentCtx).allSuggestions().map { it.asText() }
        absentSuggestions shouldNotContain "Diabetes status is in case"
        absentSuggestions shouldContain "Diabetes status is not in case"
    }

    @Test
    fun `cycle-creating derived attribute suggestions are filtered`() {
        // Given a session assigning Risk level, on which Diabetes status depends
        val ctx = SuggestionContext(
            sessionCase = materialisedCase(),
            attributes = setOf(glucose, riskLevel, diabetesStatus),
            action = ChangeTreeToAddAssignment(AssignValue(riskLevel, Literal("low"))),
            ruleTree = treeWithDependentRule(),
        )

        // When suggestions are generated
        val suggestions = ConditionSuggester(ctx).allSuggestions()

        // Then no Diabetes status suggestion is offered
        suggestions.none {
            "Diabetes status" in it.initialSuggestion().attributeNames()
        } shouldBe true
    }
}
