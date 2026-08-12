package io.rippledown.persistence

import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Rule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The ids are ordered so that two PersistentRules created with the same
// set of ids will have the same ids string. Without this, we may get
// two PersistentRules created with exactly the same data not being equal.
internal fun idsSetToString(idsSet: Set<Int>) = idsSet.toSortedSet().joinToString(",")
internal fun idsStringToIdsSet(idsString: String) = idsString.split(',').filter { it.isNotEmpty() }.map { it.toInt() }.toSet()

@Serializable
data class PersistentRule(
    val id: Int?,
    val parentId: Int?,
    val conditionIds: Set<Int>,
    val assignment: AssignValue? = null
) {

    constructor(rule: Rule) : this(
        rule.id,
        rule.parent?.id,
        rule.conditions.map { requireNotNull(it.id) { "Cannot persist a rule with an unstored condition." } }.toSet(),
        rule.assignment
    )

    constructor(
        id: Int?,
        parentId: Int?,
        conditionIdsString: String,
        assignment: AssignValue? = null
    ) : this(id, parentId, idsStringToIdsSet(conditionIdsString), assignment)

    constructor() : this(null, null, emptySet())

    fun conditionIdsString() = idsSetToString(conditionIds)

    fun assignmentString(): String? = assignment?.let { Json.encodeToString(it) }

    companion object {
        fun assignmentFromString(assignmentString: String?): AssignValue? =
            assignmentString?.let { Json.decodeFromString<AssignValue>(it) }
    }
}

interface RuleStore {
    fun all(): Set<PersistentRule>
    fun create(prototype: PersistentRule):PersistentRule
    fun load(persistentRules: Set<PersistentRule>)
    fun removeById(ruleId: Int)

    /**
     * Replace the stored rule that has the same id as the given rule.
     */
    fun update(persistentRule: PersistentRule)
}