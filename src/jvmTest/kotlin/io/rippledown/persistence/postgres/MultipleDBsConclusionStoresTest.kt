package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsConclusionStoresTest: MultipleDBsTest() {
    private lateinit var store1: ConclusionStore
    private lateinit var store2: ConclusionStore

    @BeforeTest
    override fun setup() {
        super.setup()
        store1 = kb1.conclusionStore()
        store2 = kb2.conclusionStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.conclusionStore()
        store2 = kb2.conclusionStore()
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
        val a1 = Conclusion(1, "A")
        val a2 = Conclusion(2, "B")
        val a3 = Conclusion(20, "C")
        val b1 = Conclusion(1, "A")
        val b2 = Conclusion(2, "B")
        val b3 = Conclusion(3, "C")
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

        store1.store(a11.copy(text = "AB"))
        store2.store(a21.copy(text = "AB"))
        store1.all().first { it.id == a11.id }.text shouldBe "AB"
        store2.all().first { it.id == a21.id }.text shouldBe "AB"

        reload()
        store1.all().first { it.id == a11.id }.text shouldBe "AB"
        store2.all().first { it.id == a21.id }.text shouldBe "AB"
    }
}