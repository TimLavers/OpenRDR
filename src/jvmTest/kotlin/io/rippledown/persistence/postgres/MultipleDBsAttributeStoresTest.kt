package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.persistence.AttributeStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsAttributeStoresTest: MultipleDBsTest() {
    private lateinit var store1: AttributeStore
    private lateinit var store2: AttributeStore

    @BeforeTest
    override fun setup() {
        super.setup()
        store1 = kb1.attributeStore()
        store2 = kb2.attributeStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.attributeStore()
        store2 = kb2.attributeStore()
    }

    @Test
    fun create() {
        val a11 = store1.create("ABC")
        val a21 = store2.create("ABC")
        val a12 = store1.create("DEF")
        val a22 = store2.create("GHI")
        val a23 = store2.create("XYZ")

        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22, a23)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22, a23)
    }

    @Test
    fun load() {
        val a1 = Attribute("A", 1)
        val a2 = Attribute("B", 2)
        val a3 = Attribute("C", 3)
        val b1 = Attribute("A", 10)
        val b2 = Attribute("X", 20)
        val b3 = Attribute("C", 30)
        store1.load(setOf(a1, a2, a3))
        store2.load(setOf(b1, b2, b3))

        store1.all() shouldBe setOf(a1, a2, a3)
        store2.all() shouldBe setOf(b1, b2, b3)

        reload()
        store1.all() shouldBe setOf(a1, a2, a3)
        store2.all() shouldBe setOf(b1, b2, b3)
    }

    @Test
    fun store() {
        val a11 = store1.create("ABC")
        val a21 = store2.create("ABC")
        store1.create("DEF")
        store2.create("GHI")

        store1.store(a11.copy(name = "AB"))
        store2.store(a21.copy(name = "AB"))
        store1.all().first { it.id == a11.id }.name shouldBe "AB"
        store2.all().first { it.id == a21.id }.name shouldBe "AB"

        reload()
        store1.all().first { it.id == a11.id }.name shouldBe "AB"
        store2.all().first { it.id == a21.id }.name shouldBe "AB"
    }
}