package io.rippledown.suggestions

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
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
        override fun createRuleAndAddToParent(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>) =
            Rule(100, parent, conclusion, conditions)

        override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue, conditions: Set<Condition>) =
            Rule(100, parent, null, conditions, mutableSetOf(), assignment)
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
}
