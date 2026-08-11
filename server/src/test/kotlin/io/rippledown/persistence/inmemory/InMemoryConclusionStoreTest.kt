package io.rippledown.persistence.inmemory

import io.kotest.matchers.shouldBe
import io.rippledown.model.CommentVariable
import io.rippledown.persistence.ConclusionStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryConclusionStoreTest {
    private lateinit var store: ConclusionStore

    @BeforeTest
    fun setup() {
        store = InMemoryConclusionStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptySet()
    }

    @Test
    fun `clear removes all conclusions`() {
        // Given stored conclusions
        store.create("Goats are fine.")
        store.create("Glucose is ${'$'}{}.", listOf(CommentVariable(1)))

        // When the store is cleared
        store.clear()

        // Then it is empty, and clearing again is a no-op
        store.all() shouldBe emptySet()
        store.clear()
        store.all() shouldBe emptySet()
    }

    @Test
    fun `conclusions can be created after a clear`() {
        // Given a cleared store
        store.create("Goats are fine.")
        store.clear()

        // When a conclusion is created
        val created = store.create("Sheep are fine.")

        // Then it is stored
        store.all() shouldBe setOf(created)
    }
}
