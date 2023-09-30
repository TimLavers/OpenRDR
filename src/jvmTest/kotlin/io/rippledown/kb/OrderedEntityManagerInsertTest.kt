package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import org.junit.Test

class OrderedEntityManagerInsertTest {

    @Test
    fun `insert a new entity should add it to the end of the list`() {

        val orderStore = InMemoryOrderStore()
        val manager = OrderedEntityManager(orderStore, object : EntityProvider<Attribute> {
            override fun getById(id: Int) = Attribute(id, "a")

            override fun getOrCreate(text: String) = Attribute(1, text)
        })

//        manager.insert("a")
//        manager.allInOrder() shouldBe listOf(Attribute(1, "a"))
    }


}