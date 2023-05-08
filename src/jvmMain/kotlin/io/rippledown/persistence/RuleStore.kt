package io.rippledown.persistence

// The ids are ordered so that two PersistentRules created with the same
// set of ids will have the same ids string. Without this, we may get
// two PersistentRules created with exactly the same data not being equal.
internal fun idsSetToString(idsSet: Set<Int>) = idsSet.toSortedSet().joinToString(",")
internal fun idsStringToIdsSet(idsString: String) = idsString.split(',').filter { it.isNotEmpty() }.map { it.toInt() }.toSet()

data class PersistentRule(val id: Int?, val parentId: Int?, val conclusionId: Int?, val conditionIds: Set<Int>) {

    constructor(id: Int?, parentId: Int?, conclusionId: Int?, conditionIdsString: String): this(id, parentId, conclusionId, idsStringToIdsSet(conditionIdsString) )

    constructor(): this(null, null, null, emptySet() )

    fun conditionIdsString() = idsSetToString(conditionIds)
}

interface RuleStore {
    fun all(): Set<PersistentRule>
    fun create(prototype: PersistentRule):PersistentRule
    fun load(persistentRules: Set<PersistentRule>)
}