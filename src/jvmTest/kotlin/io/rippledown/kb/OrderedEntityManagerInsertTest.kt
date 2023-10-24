package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class OrderedEntityManagerInsertTest {
    data class E(val id: Int, val name: String)

    lateinit var map: MutableMap<Int, E>

    lateinit var manager: OrderedEntityManager<E>

    @BeforeTest
    fun setup() {
        map = mutableMapOf()
        manager = OrderedEntityManager(InMemoryOrderStore(), object : EntityProvider<E> {

            override fun getById(id: Int) = map[id]!!

            override fun getOrCreate(text: String) = e(text)
        })
    }

    //Create a new entity if it's not already in the manager
    private fun e(text: String): E {
        val entity = map.values.firstOrNull { it.name == text }
        return if (entity != null) {
            entity
        } else {
            E(map.size + 1, text).apply { map[id] = this }
        }
    }

    //Create several new entities, but don't store them in the manager
    private fun l(vararg texts: String): List<E> = texts.map { e(it) }

    @Test
    fun `inserting a new entity to an empty manager should add it`() {
        //when
        manager.insert(l("a"))

        //then
        manager.allInOrder() shouldBe l("a")
    }

    @Test
    fun `inserting several new entities to an empty manager should add them in order`() {
        //when
        manager.insert(l("a", "b", "c"))

        //then
        manager.allInOrder() shouldBe l("a", "b", "c")
    }

    @Test
    fun `inserting a single new entity to a non-empty manager should append it`() {
        // Given
        manager.insert(l("a"))

        //when
        manager.insert(l("b"))

        //then
        manager.allInOrder() shouldBe l("a", "b")
    }


    @Test
    fun `should not append an entity if it's already in the manager`() {
        // Given
        manager.insert(l("a"))

        //when
        manager.insert(l("a"))

        //then
        manager.allInOrder() shouldBe l("a")
    }

    @Test
    fun `should insert a new entity before an existing entity`() {
        // Given
        manager.insert(l("a", "b"))

        //when - insert c followed by b, with b already stored
        manager.insert(l("c", "b"))

        //then
        manager.allInOrder() shouldBe l("a", "c", "b")
    }

    @Test
    fun `should insert several new entities before an existing entity`() {
        // Given
        manager.insert(l("a", "b"))

        //when - insert new c and d, with b already stored
        manager.insert(l("c", "d", "b"))

        //then
        manager.allInOrder() shouldBe l("a", "c", "d", "b")
    }

    @Test
    fun `should insert several new entities before several existing entities`() {
        // Given
        manager.insert(l("a", "b", "c"))

        //when - insert new d and e, with a and c already stored
        manager.insert(l("d", "a", "e", "c"))

        //then
        manager.allInOrder() shouldBe l("d", "a", "b", "e", "c")
    }

    @Test
    fun `should insert several new entities before several existing entities and append any other new entities`() {
        // Given
        manager.insert(l("a", "b", "c"))

        //when - insert new d and e, with a and c already stored
        manager.insert(l("d", "a", "e", "c", "f"))

        //then
        manager.allInOrder() shouldBe l("d", "a", "b", "e", "c", "f")
    }

    @Test
    fun `should insert a new entity before an existing entity and append any other new entities`() {
        // Given
        manager.insert(l("a", "b"))

        //when - insert new entities c and d, with b already stored
        manager.insert(l("c", "b", "d"))

        //then
        manager.allInOrder() shouldBe l("a", "c", "b", "d")
    }

    @Test
    fun `should find the last pair of a new entity before an existing entity`() {
        // Given
        manager.insert(l("a", "c"))

        //when - insert b followed by c, with c already stored
        val toInsert = l("b", "c")

        //then
        manager.lastNewEntityBeforeExistingEntityPair(toInsert) shouldBe Pair(e("b"), e("c"))
    }

    @Test
    fun `should find the last pair of a new entity before an existing entity when inserting several new entities`() {
        // Given
        manager.insert(l("c"))

        //when - insert new entities a and b followed by c, with c already stored
        val toInsert = l("a", "b", "c")

        //then
        manager.lastNewEntityBeforeExistingEntityPair(toInsert) shouldBe Pair(e("b"), e("c"))
    }

    @Test
    fun `should return null if there is no pair of new entity before existing entity`() {
        // Given
        manager.insert(l("a", "b", "c"))

        //when - insert b followed by c, with both b and c already stored
        val toInsert = l("b", "c")

        //then
        manager.lastNewEntityBeforeExistingEntityPair(toInsert) shouldBe null
    }

    @Test
    fun `should return null if the new entities are after all existing entities`() {
        // Given
        manager.insert(l("a", "b"))

        //when - insert new entities c and d after existing entities a and b
        val toInsert = l("a", "b", "c", "d")

        //then
        manager.lastNewEntityBeforeExistingEntityPair(toInsert) shouldBe null
    }


}