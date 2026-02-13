package io.rippledown.kb

import io.rippledown.model.Conclusion
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore

class RuleManager(
    private val conclusionManager: ConclusionManager,
    private val conditionManager: ConditionManager,
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

    override fun createRuleAndAddToParent(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>): Rule {
        println("--- create rule and add to parent ---")
        println("parent: $parent")
        println("conclusion: $conclusion")
        println("conditions: $conditions")
        val parentInTree = ruleTree.rulesMatching { it.id == parent.id }.firstOrNull()
        require(parentInTree != null) {
            "Parent rule not in tree."
        }
        // Some of the conditions may not yet exist in the KB, for example
        // if they were created as suggestions. We need to store such conditions.
        val storedConditions = conditions.map {
            if (it.id != null) it else conditionManager.getOrCreate(it)
        }.toSet()
        println("stored conditions: $storedConditions")
        val conditionIds = storedConditions.map { it.id!! }.toSet()
        val toStore = PersistentRule(null, parent.id, conclusion?.id, conditionIds)
        val stored = ruleStore.create(toStore)
        val newRule = Rule(stored.id!!, parent, conclusion, storedConditions)
        parent.addChild(newRule)
        println("--- rule created ---")
        return newRule
    }

    fun deleteLeafRule(rule: Rule) {
        rule.parent!!.removeChildLeafRule(rule)
        ruleStore.removeById(rule.id)
    }

    private fun rebuildRuleButDoNotSetParent(persistentRule: PersistentRule): Rule {
        val conclusion = if (persistentRule.conclusionId != null) conclusionManager.getById(persistentRule.conclusionId) else null
        val conditions = persistentRule.conditionIds.map { conditionManager.getById(it) }.toSet()
        return Rule(persistentRule.id!!, null, conclusion, conditions, mutableSetOf())
    }
}