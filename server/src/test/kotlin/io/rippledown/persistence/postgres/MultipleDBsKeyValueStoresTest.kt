package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.KeyValue
import io.rippledown.persistence.KeyValueStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsKeyValueStoresTest: MultipleDBsTest() {
    private lateinit var store1: KeyValueStore
    private lateinit var store2: KeyValueStore

    @BeforeTest
    override fun setup() {
        super.setup()
        store1 = kb1.metaDataStore()
        store2 = kb2.metaDataStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.metaDataStore()
        store2 = kb2.metaDataStore()
    }

    @Test
    fun create() {
        val a11 = store1.create("beach", "Bondi")
        val a21 = store2.create("beach", "Bulli")
        val a12 = store1.create("walk", "Bondi to Manly")
        val a22 = store2.create("walk", "Sublime Point")
        val a23 = store2.create("gallery", "Wollongong")

        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22, a23)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22, a23)
    }

    @Test
    fun load() {
        val a1 = KeyValue(1, "A", "apples")
        val a2 = KeyValue(2, "B", "bananas")
        val a3 = KeyValue(3, "C", "cherries")
        val b1 = KeyValue(10, "A", "accordion")
        val b2 = KeyValue(20, "X", "xylophone")
        val b3 = KeyValue(30, "C", "clarinet")
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
        val a11 = store1.create("beach", "Bondi")
        val a21 = store2.create("beach", "Bulli")
        store1.create("walk", "Bondi to Manly")
        store2.create("walk", "Sublime Point")
        store2.create("gallery", "Wollongong")

        store1.store(a11.copy(value = "Bronte"))
        store2.store(a21.copy(value = "Woonona"))
        store1.all().first { it.id == a11.id }.value shouldBe "Bronte"
        store2.all().first { it.id == a21.id }.value shouldBe "Woonona"

        reload()
        store1.all().first { it.id == a11.id }.value shouldBe "Bronte"
        store2.all().first { it.id == a21.id }.value shouldBe "Woonona"
    }
}