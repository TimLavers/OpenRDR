package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.IsHigh
import io.rippledown.persistence.ConditionStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsConditionStoresTest: MultipleDBsTest() {
    private lateinit var store1: ConditionStore
    private lateinit var store2: ConditionStore
    private val glucose1 = Attribute("Glucose", 1)
    private val tsh1 = Attribute("TSH", 2)
    private val glucose2 = Attribute("Glucose", 10)
    private val tsh2 = Attribute("TSH", 20)

    @BeforeTest
    override fun setup() {
        super.setup()
        store1 = kb1.conditionStore()
        store2 = kb2.conditionStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.conditionStore()
        store2 = kb2.conditionStore()
    }

    @Test
    fun create() {
        val a11 = store1.create(IsHigh(null, glucose1))
        val a21 = store2.create(IsHigh(null, glucose2))
        val a12 = store1.create(IsHigh(null, tsh1))
        val a22 = store2.create(IsHigh(null, tsh2))

        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
    }

    @Test
    fun load() {
        val a1 = IsHigh(1, glucose1)
        val a2 = IsHigh(2, tsh1)
        val b1 = IsHigh(1, glucose1)
        val b2 = IsHigh(2, tsh1)
        store1.load(setOf(a1, a2))
        store2.load(setOf(b1, b2))

        store1.all() shouldBe setOf(a1, a2)
        store2.all() shouldBe setOf(b1, b2)

        reload()
        store1.all() shouldBe setOf(a1, a2)
        store2.all() shouldBe setOf(b1, b2)
    }
}