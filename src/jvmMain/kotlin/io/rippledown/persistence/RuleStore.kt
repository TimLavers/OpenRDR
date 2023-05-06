package io.rippledown.persistence

import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.Rule

fun idsSetToString(idsSet: Set<Int>) = idsSet.joinToString { it.toString() }
fun idsStringToIdsSet(idsString: String) = idsString.split(',').map { it.trim()}.filter { it.isNotEmpty() }.map { it.toInt() }.toSet()

/**
 * Persistent peer of a non-root rule.
 */
data class PersistentRule(val id: Int?, val parentId: Int, val conclusionId: Int?, val conditionIds: Set<Int>) {

    constructor(id: Int?, parentId: Int, conclusionId: Int?, conditionIdsString: String): this(id, parentId, conclusionId, idsStringToIdsSet(conditionIdsString) )

    fun conditionIdsString() = idsSetToString(conditionIds)
}

/**
 * Persists the non-root rules.
 */
interface RuleStore {
    fun all(): Set<PersistentRule>
    fun create(prototype: PersistentRule):PersistentRule
    fun load(persistentRules: Set<PersistentRule>)
}