package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import kotlin.test.Test

class OrderedEntityManagerTest {
    private val a = Attribute(1, "A")
    private val b = Attribute(2, "B")
    private val c = Attribute(3, "C")
    private val d = Attribute(4, "D")
    private val e = Attribute(5, "E")

    @Test
    fun `if no ordering is defined in the orderStore, inOrder() should return a list of the entities in no specified order`() {
        val unorderedEntities = attributes()
        val manager = OrderedEntityManager(InMemoryOrderStore(), AEP(unorderedEntities))

        manager.inOrder(unorderedEntities).toSet() shouldBe unorderedEntities
    }

    @Test
    fun `if an ordering is defined in the OrderStore, inOrder() should change the ordering of the entities accordingly`() {
        val manager =  attributeOrderStoreECADB()
        manager.inOrder(attributes()) shouldBe listOf(e, c, a, d, b)
    }

    @Test
    fun `move entities`() {
        val manager =  attributeOrderStoreECADB()
        manager.move(c, e)
        manager.inOrder(attributes()) shouldBe listOf(c, e, a, d, b)
        manager.move(e, c)
        manager.inOrder(attributes()) shouldBe listOf(e, c, a, d, b)
        manager.move(d, c)
        manager.inOrder(attributes()) shouldBe listOf(e, d, c, a, b)
        manager.move(e, b)
        manager.inOrder(attributes()) shouldBe listOf(d, c, a, b, e)
    }

    @Test
    fun `move checks`() {
        val manager =  attributeOrderStoreECADB()
        shouldThrow<Exception> {
            manager.move(c, c)
        }
        val f = Attribute(99, "F")
        shouldThrow<Exception> {
            manager.move(f, c)
        }
        shouldThrow<Exception> {
            manager.move(c, f)
        }
    }

    private fun orderStore53142() = InMemoryOrderStore().apply {
        store(5, 1)
        store(3, 2)
        store(1, 3)
        store(4, 4)
        store(2, 5)
    }
    private fun attributes() =  setOf(a, b, c, d, e)
    private fun attributeOrderStoreECADB() = OrderedEntityManager(orderStore53142(), AEP(attributes()))
}
class AEP(val attributes: Set<Attribute>): EntityProvider<Attribute> {
    override fun getById(id: Int): Attribute {
        return attributes.first { it.id == id }
    }

    override fun getOrCreate(text: String) = TODO("Not required for this test")
}
