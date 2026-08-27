package io.rippledown.model.rule

import io.rippledown.model.AttributeKind
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

const val RULE_TO_ADD_COMMENT = "Add comment:"
const val RULE_TO_REMOVE_COMMENT = "Remove comment:"
const val RULE_TO_REPLACE_COMMENT = "Replace comment:"
const val RULE_TO_ASSIGN_VALUE = "Assign value:"
const val RULE_TO_RETRACT_ASSIGNMENT = "Retract assignment:"
const val RULE_TO_REPLACE_ASSIGNMENT = "Replace assignment:"
const val WITH = "with:"

open class Rule(
    val id: Int,
    var parent: Rule? = null,
    val conditions: Set<Condition> = mutableSetOf(),
    private val childRules: MutableSet<Rule> = mutableSetOf(),
    val assignment: AssignValue? = null
) {

    init {
        childRules.forEach { it.parent = this }
    }

    /**
     * What this rule does when it fires, or null if it does nothing (a
     * stopping rule, which retracts the action of its parent).
     */
    val action: RuleAction?
        get() = assignment

    fun summary(): RuleSummary {
        return RuleSummary(id, conditions, conditionTextsFromRoot(), assignment)
    }

    /**
     * How this rule reads to the user: adding, removing or replacing what
     * its parent assigned, in the wording of comments if the attribute
     * assigned is a comment attribute and of values otherwise. [describe]
     * gives the text of an assignment, and is supplied by the knowledge
     * base, which can resolve a by-definition assignment to its definition.
     */
    fun actionSummary(describe: (AssignValue) -> String = { it.asText() }): String {
        val parentRule = parent ?: return ""
        val parentAssignment = parentRule.assignment
        if (parentAssignment == null) {
            val added = assignment ?: return ""
            return "${addPrefix(added)}\n${describe(added)}"
        }
        if (assignment == null) {
            return "${removePrefix(parentAssignment)}\n${describe(parentAssignment)}"
        }
        return "${replacePrefix(assignment)}\n${describe(parentAssignment)}\n$WITH\n${describe(assignment)}"
    }

    private fun AssignValue.isComment() = attribute.kind == AttributeKind.COMMENT

    private fun addPrefix(assignment: AssignValue) =
        if (assignment.isComment()) RULE_TO_ADD_COMMENT else RULE_TO_ASSIGN_VALUE

    private fun removePrefix(assignment: AssignValue) =
        if (assignment.isComment()) RULE_TO_REMOVE_COMMENT else RULE_TO_RETRACT_ASSIGNMENT

    private fun replacePrefix(assignment: AssignValue) =
        if (assignment.isComment()) RULE_TO_REPLACE_COMMENT else RULE_TO_REPLACE_ASSIGNMENT

    fun conditionTextsFromRoot(): List<String> {
        val result = mutableListOf<String>()
        var rule: Rule? = this
        while (rule != null) {
            val sortedConditions = rule.conditions.map {
                it.asText()
            }.sortedWith(String.CASE_INSENSITIVE_ORDER)
                .asReversed()//conditions for each rule are sorted for testing only
            result.addAll(sortedConditions)
            rule = rule.parent
        }
        return result.reversed() //list the parent conditions first
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

    fun removeChildLeafRule(childLeafRule: Rule) {
        require(childLeafRule.childRules().isEmpty()) {
            "Only a leaf rule can be removed."
        }
        require(childLeafRule.parent == this) {
            "Leaf rule is not a child of this rule."
        }
        childRules.remove(childLeafRule)
        childLeafRule.parent = null
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
        val rule = Rule(id, null, conditions.toSet(), copyChildRules, assignment)
        rule.parent = parent
        return rule
    }

    override fun toString(): String {
        val sb = StringBuilder().append("Rule($id, ")
        parent?.let { sb.append("parent=$parent") }
        assignment?.let { sb.append(" assignment=$assignment") }
        if (conditions.isNotEmpty()) {
            sb.append(" conditions=$conditions")
        }
        sb.append(")")
        return sb.toString()
    }

    fun structurallyEqual(other: Rule): Boolean {
        if (assignment != other.assignment) return false
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
