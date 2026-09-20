package io.rippledown.persistence.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.isHigh
import org.junit.jupiter.api.Test

class InMemoryConditionStoreTest {
    @Test
    fun `update replaces only the existing condition with the same id`() {
        // Given
        val store = InMemoryConditionStore()
        val original = store.create(isHigh(null, Attribute(1, "Glucose"), "elevated"))
        val other = store.create(isHigh(null, Attribute(2, "TSH")))
        val renamed = original.withUserExpression("raised")

        // When
        store.update(renamed)

        // Then
        store.all() shouldBe setOf(renamed, other)
        original.userExpression() shouldBe "elevated"
    }

    @Test
    fun `update rejects missing and unknown ids without inserting anything`() {
        // Given
        val store = InMemoryConditionStore()

        // When / Then
        shouldThrow<IllegalArgumentException> { store.update(isHigh(null, Attribute(1, "Glucose"))) }
        shouldThrow<IllegalArgumentException> { store.update(isHigh(99, Attribute(1, "Glucose"))) }
        store.all() shouldBe emptySet()
    }
}
