package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.AttributeOrderStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsAttributeOrderStoresTest: MultipleDBsTest() {
    private lateinit var store1: AttributeOrderStore
    private lateinit var store2: AttributeOrderStore

    @BeforeTest
    override fun setup() {
        cleanup()
        super.setup()
        store1 = kb1.attributeOrderStore()
        store2 = kb2.attributeOrderStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.attributeOrderStore()
        store2 = kb2.attributeOrderStore()
    }

    @Test
    fun store() {
        store1.store(1, 1)
        store1.idToIndex() shouldBe mapOf(1 to 1)
        store2.idToIndex() shouldBe mapOf()

        store2.store(2, 2)
        store1.idToIndex() shouldBe mapOf(1 to 1)
        store2.idToIndex() shouldBe mapOf(2 to 2)

        store1.store(3, 3)
        store1.idToIndex() shouldBe mapOf(1 to 1, 3 to 3)
        store2.idToIndex() shouldBe mapOf(2 to 2)

        store2.store(4, 4)
        store1.idToIndex() shouldBe mapOf(1 to 1, 3 to 3)
        store2.idToIndex() shouldBe mapOf(2 to 2, 4 to 4)

        reload()
        store1.idToIndex() shouldBe mapOf(1 to 1, 3 to 3)
        store2.idToIndex() shouldBe mapOf(2 to 2, 4 to 4)
    }

    @Test
    fun load() {
        store1.load(mapOf(1 to 1, 3 to 3))
        store2.load(mapOf(2 to 1, 1 to 2))

        store1.idToIndex() shouldBe mapOf(1 to 1, 3 to 3)
        store2.idToIndex() shouldBe mapOf(2 to 1, 1 to 2)

        reload()
        store1.idToIndex() shouldBe mapOf(1 to 1, 3 to 3)
        store2.idToIndex() shouldBe mapOf(2 to 1, 1 to 2)
    }
}