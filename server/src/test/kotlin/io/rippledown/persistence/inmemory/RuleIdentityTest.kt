package io.rippledown.persistence.inmemory

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.rippledown.persistence.PersistentRule
import org.junit.jupiter.api.Test

class RuleIdentityTest {
    @Test
    fun `deleted rule ids cannot identify a different author's new rule in the change log`() {
        // Given
        val store = InMemoryRuleStore()
        val old = store.create(PersistentRule())
        store.removeById(requireNotNull(old.id))

        // When
        val replacement = store.create(PersistentRule())

        // Then
        requireNotNull(replacement.id) shouldBeGreaterThan requireNotNull(old.id)
    }

    @Test
    fun `deleted imported rule ids are not reused`() {
        // Given
        val store = InMemoryRuleStore()
        store.load(setOf(PersistentRule().copy(id = 7)))
        store.removeById(7)

        // When
        val replacement = store.create(PersistentRule())

        // Then
        requireNotNull(replacement.id) shouldBeGreaterThan 7
    }
}
