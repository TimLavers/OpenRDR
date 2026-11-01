package io.rippledown.persistence.inmemory

import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore

class InMemoryRuleStore: RuleStore {
    private val rules = mutableSetOf<PersistentRule>()
    // The change log retains deleted rule IDs and their authors.
    private var nextId = 1

    override fun all() = rules

    override fun create(prototype: PersistentRule): PersistentRule {
        require(prototype.id == null) {
            "Cannot create a persistent rule if it already has an id."
        }
        val result = prototype.copy(id = nextId++)
        rules.add(result)
        return result
    }

    override fun load(persistentRules: Set<PersistentRule>) {
        require(rules.isEmpty()) {
            "Cannot load a rule store if it already has rules in it."
        }
        persistentRules.forEach { require(it.id != null) {"Cannot load a rule that has null id."} }
        rules.addAll(persistentRules)
        nextId = maxOf(nextId, (persistentRules.maxOfOrNull { it.id ?: 0 } ?: 0) + 1)
    }

    override fun removeById(ruleId: Int) {
        rules.removeIf { it.id == ruleId }
    }

    override fun update(persistentRule: PersistentRule) {
        require(persistentRule.id != null) {
            "Cannot update a rule that has no id."
        }
        require(rules.any { it.id == persistentRule.id }) {
            "Cannot update a rule that is not in the store."
        }
        rules.removeIf { it.id == persistentRule.id }
        rules.add(persistentRule)
    }
}
