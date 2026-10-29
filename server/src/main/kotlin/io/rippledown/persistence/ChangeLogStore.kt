package io.rippledown.persistence

import io.rippledown.model.change.ChangeRecord

/**
 * Append-only store of a knowledge base's change records.
 * See documentation/design/concurrent_users.md.
 */
interface ChangeLogStore {
    /** Every record, oldest first. */
    fun all(): List<ChangeRecord>

    /** Stores [record], ignoring any id it has, and returns it with the id assigned. */
    fun create(record: ChangeRecord): ChangeRecord

    fun load(data: Set<ChangeRecord>)
}

fun idsToString(ids: Set<Int>) = ids.joinToString(" ")

fun idsFromString(text: String): Set<Int> =
    text.split(' ').filter { it.isNotBlank() }.map { it.trim().toInt() }.toSet()
