package io.rippledown.persistence.inmemory

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.rippledown.model.rule.RuleSessionRecord
import org.junit.jupiter.api.Test

class RuleSessionRecordIdentityTest {
    @Test
    fun `deleting all records does not allow a new session to reuse an old identity`() {
        // Given
        val store = InMemoryRuleSessionRecordStore()
        val first = store.create(RuleSessionRecord(null, 1, setOf(2)))
        store.deleteImpl(first)

        // When
        val next = store.create(RuleSessionRecord(null, 1, setOf(2)))

        // Then
        requireNotNull(next.id) shouldBeGreaterThan requireNotNull(first.id)
    }

    @Test
    fun `deleted imported sessions also reserve their identities`() {
        // Given
        val store = InMemoryRuleSessionRecordStore()
        store.load(setOf(RuleSessionRecord(3, 1, setOf(2)), RuleSessionRecord(7, 2, setOf(3))))
        store.deleteLastAdded()

        // When
        val next = store.create(RuleSessionRecord(null, 1, setOf(4)))

        // Then
        requireNotNull(next.id) shouldBeGreaterThan 7
    }
}
