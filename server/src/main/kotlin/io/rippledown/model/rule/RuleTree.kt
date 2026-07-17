package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import kotlin.random.Random

fun rootRule(): Rule {
    return Rule(0)
}

open class RuleTree(val root: Rule = rootRule()) {

    /**
     * Interpret the case by fixpoint iteration: strip derived values, then
     * repeatedly evaluate the tree and materialise the derived-attribute
     * assignments made by the rules that fired, until the interpretation
     * and the derived values are stable. Termination is guaranteed because
     * the derived-attribute dependency graph is kept acyclic at rule-build
     * time. The case's interpretation is updated in place; [materialise]
     * additionally returns the case with its derived values written.
     */
    fun apply(kase: RDRCase): Interpretation {
        materialise(kase)
        return kase.interpretation
    }

    fun materialise(kase: RDRCase): RDRCase {
        val base = kase.withoutDerivedValues()
        var current = base
        while (true) {
            current.resetInterpretation()
            root.childRules().forEach { it.apply(current, current.interpretation) }//don't include the root conclusion
            val next = materialiseAssignments(base, current)
            // Rule evaluation is a pure function of the case data, so if the
            // derived values are unchanged, the interpretation is stable too.
            if (next.hasSameDataAs(current)) return next
            current = next
        }
    }

    /**
     * The stripped base case with the assignments made by the rules that
     * fired written into its latest episode. Rebuilding from the base each
     * pass ensures that an assignment retracted in a later pass leaves no
     * stale value. Expressions are evaluated against the case as it stood
     * during the pass, so an expression referencing an attribute assigned
     * in the same pass resolves on a later pass.
     */
    private fun materialiseAssignments(base: RDRCase, evaluated: RDRCase): RDRCase {
        if (base.numberOfEpisodes() == 0) return base
        var result = base
        evaluated.interpretation.assignments()
            .sortedBy { it.attribute.id } // deterministic write order
            .forEach { assignment ->
                assignment.expression.evaluate(evaluated)?.let {
                    result = result.withDerivedValue(assignment.attribute, it)
                }
            }
        return result
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

    open fun rule(conclusion: Conclusion?, conditions: Set<Condition>): Rule {
        return Rule(Random.Default.nextInt(), null, conclusion, conditions)
    }
}