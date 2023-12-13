package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import kotlin.test.Test

class OrderedEntityManagerTest {
    @Test
    fun `if no ordering is defined in the orderStore, inOrder() should return a list of the entities in no specified order`() {

        val unorderedEntities = setOf(
            Attribute(1, "a"), Attribute(2, "b"), Attribute(3, "c")
        )
        val orderStore = InMemoryOrderStore()

        val manager = OrderedEntityManager(orderStore, object : EntityProvider<Attribute> {
            override fun getById(id: Int) = TODO("Not required for this test")

            override fun getOrCreate(text: String): Attribute {
                return unorderedEntities.first { it.name == text }
            }
        })

        manager.inOrder(unorderedEntities).toSet() shouldBe unorderedEntities
    }

    @Test
    fun `if an ordering is defined in the OrderStore, inOrder() should change the ordering of the entities accordingly`() {

        val unorderedEntities = setOf(
            Attribute(1, "a"), Attribute(2, "b"), Attribute(3, "c")
        )
        val orderStore = InMemoryOrderStore().apply {
            store(3, 1);
            store(1, 2);
            store(2, 3)
        }

        val manager = OrderedEntityManager(orderStore, object : EntityProvider<Attribute> {
            override fun getById(id: Int): Attribute {
                return unorderedEntities.first { it.id == id }
            }

            override fun getOrCreate(text: String) = TODO("Not required for this test")
        })

        manager.inOrder(unorderedEntities) shouldBe listOf(
            Attribute(3, "c"), Attribute(1, "a"), Attribute(2, "b")
        )
    }
}