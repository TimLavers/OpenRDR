package io.rippledown.model.rule

import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.util.randomString

fun rootRule(): Rule {
    return Rule("root")
}

open class RuleTree(val root: Rule = rootRule()) {

    fun apply(kase: RDRCase): Interpretation {
        val interpretation = Interpretation(CaseId(kase.name, kase.name))
        root.childRules().forEach { it.apply(kase, interpretation) }//don't include the root conclusion
        return interpretation
    }

    //Note that the root is counted
    fun size(): Long {
        var result = 0L
        root.visit {
            result++
        }
        return result
    }

    //Note that the root is not returned
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
        return Rule(randomString(8), null, conclusion, conditions)
    }
}