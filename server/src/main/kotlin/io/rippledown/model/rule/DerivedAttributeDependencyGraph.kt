package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.condition.Condition

/**
 * The dependency graph of the derived attributes in a rule tree, used to
 * keep dependencies acyclic so that fixpoint inference terminates. See
 * "Stratification: keeping dependencies acyclic" in
 * documentation/design/repeat_inferencing.md.
 *
 * Nodes are derived attributes. Derived attribute B depends on derived
 * attribute A if some rule whose action assigns B (or removes or replaces
 * an assignment of B) has a condition referring to A anywhere on its path
 * from the root, or has a value expression referring to A.
 *
 * The graph is computed on demand from the rule tree; it is not persisted.
 */
class DerivedAttributeDependencyGraph(ruleTree: RuleTree, knownAttributes: Set<Attribute>) {
    private val derivedByName = knownAttributes
        .filter { it.kind == AttributeKind.DERIVED }
        .associateBy { it.name }

    private val dependencies: Map<Attribute, Set<Attribute>> = buildDependencies(ruleTree)

    /**
     * The cycle that would be created by building a rule for the given
     * action with the given condition, as a path from the action's assigned
     * attribute back to itself, or null if there would be no cycle.
     */
    fun cycleCreatedBy(action: RuleTreeChange?, condition: Condition?): List<Attribute>? {
        val assigned = action?.assignedAttribute() ?: return null
        val referenced = derivedIn(action.expressionReferences()) +
                (condition?.let { derivedAttributesIn(it) } ?: emptySet())
        return cycleCreatedBy(assigned, referenced)
    }

    /**
     * The cycle that would be created by adding edges from [assigned] to
     * each of [referenced], or null if there would be no cycle.
     */
    fun cycleCreatedBy(assigned: Attribute, referenced: Set<Attribute>): List<Attribute>? {
        if (referenced.isEmpty()) return null
        val edges = dependencies.mapValues { it.value.toMutableSet() }.toMutableMap()
        edges.getOrPut(assigned) { mutableSetOf() }.addAll(referenced)
        return pathBackToStart(assigned, edges)
    }

    private fun buildDependencies(ruleTree: RuleTree): Map<Attribute, Set<Attribute>> {
        val result = mutableMapOf<Attribute, MutableSet<Attribute>>()
        ruleTree.rules().forEach { rule ->
            val assigned = attributeAffectedBy(rule) ?: return@forEach
            val dependsOn = result.getOrPut(assigned) { mutableSetOf() }
            conditionsOnPathFromRoot(rule).forEach { condition ->
                dependsOn.addAll(derivedAttributesIn(condition))
            }
            rule.assignment?.let { dependsOn.addAll(derivedIn(it.expression.referencedAttributes())) }
        }
        return result
    }

    private fun attributeAffectedBy(rule: Rule) =
        rule.assignment?.attribute ?: rule.parent?.assignment?.attribute

    private fun conditionsOnPathFromRoot(rule: Rule): List<Condition> {
        val result = mutableListOf<Condition>()
        var current: Rule? = rule
        while (current != null) {
            result.addAll(current.conditions)
            current = current.parent
        }
        return result
    }

    private fun derivedAttributesIn(condition: Condition) =
        condition.attributeNames().mapNotNull { derivedByName[it] }.toSet()

    private fun derivedIn(attributes: Set<Attribute>) =
        attributes.mapNotNull { derivedByName[it.name] }.toSet()

    /**
     * A path from [start] back to itself in the graph given by [edges],
     * or null if there is none.
     */
    private fun pathBackToStart(start: Attribute, edges: Map<Attribute, Set<Attribute>>): List<Attribute>? {
        val path = mutableListOf(start)
        val visited = mutableSetOf<Attribute>()

        fun search(node: Attribute): Boolean {
            edges[node].orEmpty().forEach { next ->
                if (next == start) {
                    path.add(start)
                    return true
                }
                if (visited.add(next)) {
                    path.add(next)
                    if (search(next)) return true
                    path.removeAt(path.size - 1)
                }
            }
            return false
        }
        return if (search(start)) path else null
    }
}

/**
 * The message explaining why a cycle-creating condition or assignment is
 * refused, naming the cycle.
 */
fun cycleMessage(cycle: List<Attribute>): String {
    val cycleText = cycle.joinToString(" → ") { it.name }
    return "it would make \"${cycle.first().name}\" depend on itself ($cycleText)"
}
