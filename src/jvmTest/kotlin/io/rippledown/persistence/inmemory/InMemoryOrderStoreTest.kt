package io.rippledown.persistence.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.persistence.inmemory.InMemoryOrderStore.Companion.ERROR_MESSAGE
import kotlin.test.Test

class InMemoryOrderStoreTest {
    @Test
    fun `should store an id and its index`() {
        with(InMemoryOrderStore()) {
            // given
            idToIndex() shouldBe emptyMap()

            // when
            store(0, 42)

            // then
            idToIndex() shouldBe mapOf(0 to 42)
        }
    }

    @Test
    fun `should load ids and their indexes`() {
        with(InMemoryOrderStore()) {

            // given
            idToIndex() shouldBe emptyMap()

            // when
            val map = mapOf(0 to 42, 1 to 43)
            load(map)

            // then
            idToIndex() shouldBe map
        }
    }

    @Test
    fun `should not load if the store is not empty`() {
        with(InMemoryOrderStore()) {
            // given
            store(0, 42)

            // when
            val map = mapOf(1 to 43)

            // then
            shouldThrow<IllegalArgumentException> {
                load(map)
            } shouldBe IllegalArgumentException(ERROR_MESSAGE)
        }
    }
}