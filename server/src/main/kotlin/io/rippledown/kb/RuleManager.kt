package io.rippledown.kb

import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore

class RuleManager(
    private val conditionManager: ConditionManager,
    private val attributeProvider: AttributeProvider,
    private val ruleStore: RuleStore
) : RuleFactory {

    private val ruleTree: RuleTree

    init {
        // Create the root rule if necessary.
        if (ruleStore.all().isEmpty()) {
            ruleStore.create(PersistentRule())
        }
        // Partially rebuild all of the stored rules.
        val idToPersistentRule = ruleStore.all().associateBy { it.id }
        val idToRule = ruleStore.all().associate {it.id to rebuildRuleButDoNotSetParent(it) }

        // Now set the parent rules for the partially rebuilt rules.
        idToPersistentRule.forEach{
            if (it.value.parentId != null) {
                val rule = idToRule[it.key]!!
                val parent = idToRule[it.value.parentId]!!
                parent.addChild(rule)
            }
        }

        val rulesWithoutParent = idToRule.values.filter { it.parent == null }
        require(rulesWithoutParent.size == 1) {
            "Rule tree could not be rebuilt as more than one rule lacks a parent."
        }
        val root = rulesWithoutParent.single()
        ruleTree = RuleTree(root)
    }

    fun ruleTree() = ruleTree

    override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>): Rule {
        val parentInTree = ruleTree.rulesMatching { it.id == parent.id }.firstOrNull()
        require(parentInTree != null) {
            "Parent rule not in tree."
        }
        val storedConditions = storeConditionsAsNeeded(conditions)
        val conditionIds = conditionIds(storedConditions)
        val toStore = PersistentRule(null, parent.id, conditionIds, assignment)
        val stored = ruleStore.create(toStore)
        val newRule = Rule(storedRuleId(stored), parent, storedConditions, mutableSetOf(), assignment)
        parent.addChild(newRule)
        return newRule
    }

    // Some of the conditions may not yet exist in the KB, for example
    // if they were created as suggestions. We need to store such conditions.
    private fun storeConditionsAsNeeded(conditions: Set<Condition>) = conditions.map {
        if (it.id != null) it else conditionManager.getOrCreate(it)
    }.toSet()

    private fun conditionIds(storedConditions: Set<Condition>) = storedConditions.map {
        requireNotNull(it.id) { "Stored condition has no id." }
    }.toSet()

    private fun storedRuleId(stored: PersistentRule) =
        checkNotNull(stored.id) { "Stored rule has no id." }

    fun deleteLeafRule(rule: Rule) {
        val parent = requireNotNull(rule.parent) { "Cannot delete the root rule." }
        parent.removeChildLeafRule(rule)
        ruleStore.removeById(rule.id)
    }

    private fun rebuildRuleButDoNotSetParent(persistentRule: PersistentRule): Rule {
        val conditions = persistentRule.conditionIds.map { conditionManager.getById(it) }.toSet()
        return Rule(
            storedRuleId(persistentRule),
            null,
            conditions,
            mutableSetOf(),
            aligned(persistentRule.assignment)
        )
    }

    /**
     * The stored assignment with its attributes replaced by those held by the
     * attribute manager, so that a rule built before an attribute was renamed
     * shows the attribute's current name. A missing attribute is inconsistent
     * persisted state and prevents the knowledge base loading.
     */
    private fun aligned(assignment: AssignValue?): AssignValue? {
        if (assignment == null) return null
        return assignment.alignAttributes { id -> attributeProvider.getById(id) }
    }
}