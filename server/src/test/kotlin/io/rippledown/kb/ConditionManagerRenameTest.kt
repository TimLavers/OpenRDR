package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.condition.isHigh
import io.rippledown.persistence.ConditionStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConditionStore
import org.junit.jupiter.api.Test

class ConditionManagerRenameTest {
    private val attributes = AttributeManager(InMemoryAttributeStore())
    private val glucose = attributes.getOrCreate("Glucose")
    private val store = InMemoryConditionStore()
    private val manager = ConditionManager(attributes, store)

    @Test
    fun `rename updates the cache persistence and subsequent deduplication`() {
        // Given
        val original = manager.getOrCreate(isHigh(null, glucose, "elevated"))
        val id = requireNotNull(original.id)

        // When
        val renamed = manager.renamePhrase(id, "raised")

        // Then
        renamed.userExpression() shouldBe "raised"
        manager.getById(id) shouldBeSameInstanceAs renamed
        manager.getOrCreate(isHigh(null, glucose, "another phrase")) shouldBeSameInstanceAs renamed
        ConditionManager(attributes, store).getById(id) shouldBe renamed
        original.userExpression() shouldBe "elevated"
    }

    @Test
    fun `blank phrases and unknown ids leave the stored condition intact`() {
        // Given
        val original = manager.getOrCreate(isHigh(null, glucose, "elevated"))

        // When / Then
        listOf("", " \t\n").forEach { phrase ->
            shouldThrow<IllegalArgumentException> { manager.renamePhrase(requireNotNull(original.id), phrase) }
        }
        shouldThrow<NoSuchElementException> { manager.renamePhrase(99, "raised") }
        manager.all() shouldBe setOf(original)
        store.all() shouldBe setOf(original)
    }

    @Test
    fun `a persistence failure leaves the cache unchanged`() {
        // Given
        val original = isHigh(1, glucose, "elevated")
        val failingStore = mockk<ConditionStore>()
        every { failingStore.all() } returns setOf(original)
        every { failingStore.update(any()) } throws IllegalStateException("write failed")
        val manager = ConditionManager(attributes, failingStore)

        // When / Then
        shouldThrow<IllegalStateException> { manager.renamePhrase(1, "raised") }.message shouldBe "write failed"
        manager.getById(1).userExpression() shouldBe "elevated"
    }
}
