package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.isHigh
import io.rippledown.persistence.ConditionStore
import io.rippledown.util.shouldBeSameAs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsConditionStoresTest : MultipleDBsTest() {
    private lateinit var store1: ConditionStore
    private lateinit var store2: ConditionStore
    private val glucose1 = Attribute(1, "Glucose")
    private val tsh1 = Attribute(2, "TSH")
    private val glucose2 = Attribute(10, "Glucose")
    private val tsh2 = Attribute(20, "TSH")

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
        val a11 = store1.create(isHigh(null, glucose1))
        val a21 = store2.create(isHigh(null, glucose2))
        val a12 = store1.create(isHigh(null, tsh1))
        val a22 = store2.create(isHigh(null, tsh2))

        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
    }

    @Test
    fun `a created condition should be the same as the original`() {
        // Given
        val original = isHigh(null, glucose1)

        // When
        val created = store1.create(original)

        // Then
        created shouldBeSameAs original
    }

    @Test
    fun `a created condition should have the same user expression as the original`() {
        // Given
        val userExpression = "Elevated glucose"
        val original = isHigh(null, glucose1, userExpression)

        // When
        val created = store1.create(original)

        // Then
        created shouldBeSameAs original
        created.userExpression() shouldBe userExpression
    }

    @Test
    fun load() {
        val a1 = isHigh(1, glucose1)
        val a2 = isHigh(2, tsh1)
        val b1 = isHigh(1, glucose1)
        val b2 = isHigh(2, tsh1)
        store1.load(setOf(a1, a2))
        store2.load(setOf(b1, b2))

        store1.all() shouldBe setOf(a1, a2)
        store2.all() shouldBe setOf(b1, b2)

        reload()
        store1.all() shouldBe setOf(a1, a2)
        store2.all() shouldBe setOf(b1, b2)
    }
}