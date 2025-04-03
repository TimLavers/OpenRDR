package io.rippledown.persistence

import io.rippledown.model.rule.Rule
import kotlinx.serialization.Serializable

// The ids are ordered so that two PersistentRules created with the same
// set of ids will have the same ids string. Without this, we may get
// two PersistentRules created with exactly the same data not being equal.
internal fun idsSetToString(idsSet: Set<Int>) = idsSet.toSortedSet().joinToString(",")
internal fun idsStringToIdsSet(idsString: String) = idsString.split(',').filter { it.isNotEmpty() }.map { it.toInt() }.toSet()

@Serializable
data class PersistentRule(val id: Int?, val parentId: Int?, val conclusionId: Int?, val conditionIds: Set<Int>) {

    constructor(rule: Rule): this(rule.id, rule.parent?.id, rule.conclusion?.id, rule.conditions.map { it.id!! }.toSet())

    constructor(id: Int?, parentId: Int?, conclusionId: Int?, conditionIdsString: String): this(id, parentId, conclusionId, idsStringToIdsSet(conditionIdsString) )

    constructor(): this(null, null, null, emptySet() )

    fun conditionIdsString() = idsSetToString(conditionIds)
}

interface RuleStore {
    fun all(): Set<PersistentRule>
    fun create(prototype: PersistentRule):PersistentRule
    fun load(persistentRules: Set<PersistentRule>)
    fun remove(persistentRule: PersistentRule)
}