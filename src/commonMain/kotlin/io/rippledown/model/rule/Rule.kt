package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

open class Rule(
    val id: Int,
    var parent: Rule? = null,
    val conclusion: Conclusion? = null,
    val conditions: Set<Condition> = mutableSetOf(),
    private val childRules: MutableSet<Rule> = mutableSetOf()) {

    init {
        childRules.forEach { it.parent = this }
    }

    fun summary(): RuleSummary {
        return RuleSummary(id, conclusion, conditions)
    }

    fun conditionsSatisfied(case: RDRCase): Boolean {
        conditions.forEach { condition: Condition ->
            if (!condition.holds(case)) {
                return false
            }
        }
        return true
    }

    fun childRules(): Set<Rule> {
        return childRules.toSet()
    }

    fun addChild(childRule: Rule) {
        childRules.add(childRule)
        childRules.forEach { it.parent = this }
    }

    fun visit(action: (Rule) -> Unit) {
        action(this)
        childRules.forEach { it.visit(action) }
    }

    fun apply(kase: RDRCase, interpretation: Interpretation): Boolean {
        if (!conditionsSatisfied(kase)) return false
        var childRuleApplied = false
        childRules().forEach {
            childRuleApplied = it.apply(kase, interpretation) || childRuleApplied
        }
        if (!childRuleApplied) interpretation.add(this)
        return true
    }

    fun copy(): Rule {
        val copyChildRules = mutableSetOf<Rule>()
        childRules().forEach { r -> copyChildRules.add(r.copy()) }
        val rule = Rule(id,null, conclusion?.copy(), conditions.toSet(), copyChildRules)
        rule.parent = parent
        return rule
    }

    override fun toString(): String {
        val sb = StringBuilder().append("Rule($id, ")
        parent?.let { sb.append("parent=$parent") }
        conclusion?.let { sb.append(" conclusion=$conclusion") }
        if (conditions.isNotEmpty()) {
            sb.append(" conditions=$conditions")
        }
        sb.append(")")
        return sb.toString()
    }

    fun structurallyEqual(other: Rule): Boolean {
        if (conclusion != other.conclusion) return false
        if (conditions != other.conditions) return false
        return parent == other.parent
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Rule

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
