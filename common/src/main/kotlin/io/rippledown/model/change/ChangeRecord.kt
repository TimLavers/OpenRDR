package io.rippledown.model.change

import kotlinx.serialization.Serializable

@Serializable
enum class ChangeKind {
    RULES_ADDED,
    RULES_UNDONE,
    DEFINITION_EDITED,
    ATTRIBUTE_RENAMED,
    CONDITION_RENAMED,
    ATTRIBUTE_MOVED,
    CASE_DELETED,
    DESCRIPTION_SET
}

/**
 * One entry in a knowledge base's change log: who changed what, and when.
 * [summary] is a single line written for a colleague reading the history.
 * [attributeIds] are the attributes the change concerns, so that a question
 * such as "who changed BMI?" is a filter by id and survives a rename.
 * See documentation/design/concurrent_users.md.
 */
@Serializable
data class ChangeRecord(
    val id: Int?,
    val userId: String,
    val timestamp: Long,
    val kind: ChangeKind,
    val summary: String,
    val ruleIds: Set<Int> = emptySet(),
    val attributeIds: Set<Int> = emptySet()
)

@Serializable
data class KbChange(val kbId: String, val record: ChangeRecord)
