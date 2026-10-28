package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.rule.evaluation.DefaultValueExpressionEvaluator
import io.rippledown.model.rule.evaluation.EvaluationContext
import io.rippledown.model.rule.evaluation.EvaluationOutcome
import io.rippledown.model.rule.evaluation.ValueExpressionEvaluator
import kotlin.random.Random

fun rootRule(): Rule {
    return Rule(0)
}

/**
 * Resolves a derived attribute to its stored value-expression definition,
 * used to substitute [ByDefinition] rule actions with the concrete
 * expression before evaluation. See
 * documentation/design/derived_attribute_definitions.md.
 */
typealias DefinitionResolver = (Attribute) -> ValueExpression?

val NO_DEFINITIONS: DefinitionResolver = { null }

/**
 * This expression with a [ByDefinition] sentinel replaced by the stored
 * definition of [attribute], or null if there is no stored definition.
 * A concrete expression resolves to itself.
 */
fun ValueExpression.resolvedFor(attribute: Attribute, resolver: DefinitionResolver): ValueExpression? =
    if (this is ByDefinition) resolver(attribute) else this

/**
 * How an inference run ended. Only [Complete] is an interpretation. A
 * [Demand] asks the coordinator to obtain one classification and run again;
 * [Incomplete] reports that a classification failed. In both of the latter
 * the case's interpretation has been reset, so no partial result can be
 * published by mistake. See documentation/tickets/ai_derived_attributes_design.md.
 */
sealed class InferenceOutcome {
    data class Complete(val case: RDRCase, val provenance: Map<Attribute, AiProvenance>) : InferenceOutcome()

    sealed class Stopped : InferenceOutcome() {
        abstract val description: String
    }

    data class Demand(val attribute: Attribute, val request: AiEvaluationRequest) : Stopped() {
        override val description
            get() = "Interpretation needs an AI evaluation of ${attribute.name} that has not been prepared."
    }

    data class Incomplete(val attribute: Attribute, val reason: String) : Stopped() {
        override val description get() = "The AI evaluation of ${attribute.name} failed: $reason"
    }
}

/**
 * Thrown by the synchronous entry points when inference cannot complete
 * without a classification that has not been prepared.
 */
class IncompleteInferenceException(val outcome: InferenceOutcome.Stopped) : IllegalStateException(outcome.description)

open class RuleTree(val root: Rule = rootRule()) {

    fun replaceCondition(condition: Condition) {
        root.visit { it.replaceCondition(condition) }
    }

    /**
     * Interpret the case by fixpoint iteration: strip derived values, then
     * repeatedly evaluate the tree and materialise the derived-attribute
     * assignments made by the rules that fired, until the interpretation
     * and the derived values are stable. Termination is guaranteed because
     * the derived-attribute dependency graph is kept acyclic at rule-build
     * time. The case's interpretation is updated in place; [materialise]
     * additionally returns the case with its derived values written.
     */
    fun apply(
        kase: RDRCase,
        resolver: DefinitionResolver = NO_DEFINITIONS,
        context: EvaluationContext = EvaluationContext.EMPTY
    ): Interpretation {
        materialise(kase, resolver, context)
        return kase.interpretation
    }

    /**
     * Interpretation with prepared results only: an AI evaluation that is not
     * in [context] is an error here, not a missing value. The coordinator
     * catches it, obtains the result outside the lock and runs again.
     */
    fun materialise(
        kase: RDRCase,
        resolver: DefinitionResolver = NO_DEFINITIONS,
        context: EvaluationContext = EvaluationContext.EMPTY
    ): RDRCase =
        when (val outcome = infer(kase, resolver, context = context)) {
            is InferenceOutcome.Complete -> outcome.case
            is InferenceOutcome.Stopped -> throw IncompleteInferenceException(outcome)
        }

    /**
     * The fixpoint iteration with prepared AI results. The engine performs
     * no I/O: an expression needing a result not in [context] ends the run
     * with a [InferenceOutcome.Demand], and the coordinator supplies the
     * result and calls again. Every pass of one run reads the same context.
     */
    fun infer(
        kase: RDRCase,
        resolver: DefinitionResolver = NO_DEFINITIONS,
        evaluator: ValueExpressionEvaluator = DefaultValueExpressionEvaluator(),
        context: EvaluationContext = EvaluationContext.EMPTY
    ): InferenceOutcome {
        val base = kase.withoutDerivedValues()
        var current = base
        while (true) {
            current.resetInterpretation()
            root.childRules().forEach { it.apply(current, current.interpretation) }//don't include the root conclusion
            when (val pass = materialiseAssignments(base, current, resolver, evaluator, context)) {
                is InferenceOutcome.Stopped -> {
                    kase.resetInterpretation()
                    return pass
                }
                // Rule evaluation is a pure function of the case data, so if the
                // derived values are unchanged, the interpretation is stable too.
                is InferenceOutcome.Complete -> {
                    if (pass.case.hasSameDataAs(current)) return pass
                    current = pass.case
                }
            }
        }
    }

    /**
     * The stripped base case with the assignments made by the rules that
     * fired written into its latest episode. Rebuilding from the base each
     * pass ensures that an assignment retracted in a later pass leaves no
     * stale value. Expressions are evaluated against the case as it stood
     * during the pass, so an expression referencing an attribute assigned
     * in the same pass resolves on a later pass. A [ByDefinition] action is
     * first substituted with the attribute's stored definition; if there is
     * no definition, no assignment is made.
     */
    private fun materialiseAssignments(
        base: RDRCase,
        evaluated: RDRCase,
        resolver: DefinitionResolver,
        evaluator: ValueExpressionEvaluator,
        context: EvaluationContext
    ): InferenceOutcome {
        if (base.numberOfEpisodes() == 0) return InferenceOutcome.Complete(base, emptyMap())
        var result = base
        val provenance = mutableMapOf<Attribute, AiProvenance>()
        evaluated.interpretation.assignments()
            .sortedBy { it.attribute.id } // deterministic write order
            .forEach { assignment ->
                val expression = assignment.expression.resolvedFor(assignment.attribute, resolver) ?: return@forEach
                when (val outcome = evaluator.evaluate(expression, evaluated, context)) {
                    is EvaluationOutcome.Value -> {
                        result = result.withDerivedValue(assignment.attribute, outcome.value)
                        outcome.provenance?.let { provenance[assignment.attribute] = it }
                    }
                    EvaluationOutcome.NoInput -> Unit
                    is EvaluationOutcome.NeedsEvaluation -> return InferenceOutcome.Demand(assignment.attribute, outcome.request)
                    is EvaluationOutcome.Failed -> return InferenceOutcome.Incomplete(assignment.attribute, outcome.reason)
                }
            }
        return InferenceOutcome.Complete(result, provenance)
    }

    //Note that the root is counted
    fun size(): Long {
        var result = 0L
        root.visit {
            result++
        }
        return result
    }

    fun rules(): Set<Rule> {
        val result = mutableSetOf<Rule>()
        root.visit {
            result.add(it)
        }
        return result
    }

    fun rulesMatching(predicate: ((Rule) -> Boolean)): Set<Rule> {
        val result = mutableSetOf<Rule>()
        val action: ((Rule) -> (Unit)) = { rule ->
            if (predicate(rule)) {
                result.add(rule)
            }
        }
        root.visit(action)
        return result
    }

    fun ruleForId(id: Int) = rulesMatching { it.id == id }.first()

    fun copy(): RuleTree {
        return RuleTree(root.copy())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RuleTree && root == other.root
    }

    override fun hashCode(): Int {
        return root.hashCode()
    }

    override fun toString(): String {
        return "RuleTree(root=$root)"
    }

    open fun rule(assignment: AssignValue?, conditions: Set<Condition>): Rule {
        return Rule(Random.Default.nextInt(), null, conditions, mutableSetOf(), assignment)
    }
}
