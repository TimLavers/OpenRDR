package io.rippledown.persistence.inmemory

import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore

class InMemoryRuleStore: RuleStore {
    private val rules = mutableSetOf<PersistentRule>()

    override fun all() = rules

    override fun create(prototype: PersistentRule): PersistentRule {
        require(prototype.id == null) {
            "Cannot create a persistent rule if it already has an id."
        }
        val maxById = rules.maxByOrNull { it.id!! }
        val maxId = maxById?.id ?: 0
        val result = prototype.copy(id = maxId + 1)
        rules.add(result)
        return result
    }

    override fun load(persistentRules: Set<PersistentRule>) {
        require(rules.isEmpty()) {
            "Cannot load a rule store if it already has rules in it."
        }
        persistentRules.forEach { require(it.id != null) {"Cannot load a rule that has null id."} }
        rules.addAll(persistentRules)
    }
}