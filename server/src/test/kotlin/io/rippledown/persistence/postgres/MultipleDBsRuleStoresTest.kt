package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsRuleStoresTest: MultipleDBsTest() {
    private lateinit var store1: RuleStore
    private lateinit var store2: RuleStore

    @BeforeTest
    override fun setup() {
        super.setup()
        store1 = kb1.ruleStore()
        store2 = kb2.ruleStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.ruleStore()
        store2 = kb2.ruleStore()
    }

    @Test
    fun create() {
        val a11 = store1.create(PersistentRule(null, null, null, setOf()))
        val a21 = store2.create(PersistentRule(null, null, null, setOf()))
        val a12 = store1.create(PersistentRule(null, 0, 5, setOf(10, 11)))
        val a22 = store2.create(PersistentRule(null, 0, 6, setOf(11, 12)))

        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
    }

    @Test
    fun remove() {
        val a11 = store1.create(PersistentRule(null, null, null, setOf()))
        val a21 = store2.create(PersistentRule(null, null, null, setOf()))
        val a12 = store1.create(PersistentRule(null, 0, 5, setOf(10, 11)))
        val a22 = store2.create(PersistentRule(null, 0, 6, setOf(11, 12)))

        store1.remove(a12)
        store2.remove(a22)
        store1.all() shouldBe setOf(a11)
        store2.all() shouldBe setOf(a21)
        reload()
        store1.all() shouldBe setOf(a11)
        store2.all() shouldBe setOf(a21)
    }

    @Test
    fun load() {
        val a1 = PersistentRule(0, null, null, setOf())
        val a2 = PersistentRule(1, 0, 9, setOf(7, 8))
        val b1 = PersistentRule(0, null, null, setOf())
        val b2 = PersistentRule(1, 0, 9, setOf(17, 18))
        store1.load(setOf(a1, a2))
        store2.load(setOf(b1, b2))

        store1.all() shouldBe setOf(a1, a2)
        store2.all() shouldBe setOf(b1, b2)

        reload()
        store1.all() shouldBe setOf(a1, a2)
        store2.all() shouldBe setOf(b1, b2)
    }
}