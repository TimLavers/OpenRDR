package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsRuleSessionRecordStoresTest: MultipleDBsTest() {
    private lateinit var store1: RuleSessionRecordStore
    private lateinit var store2: RuleSessionRecordStore

    @BeforeTest
    override fun setup() {
        super.setup()
        store1 = kb1.ruleSessionRecordStore()
        store2 = kb2.ruleSessionRecordStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.ruleSessionRecordStore()
        store2 = kb2.ruleSessionRecordStore()
    }

    @Test
    fun create() {
        val a11 = store1.create(RuleSessionRecord(null, 1, setOf(1,2)))
        val a21 = store2.create(RuleSessionRecord(null, 1, setOf(2,3)))
        val a12 = store1.create(RuleSessionRecord(null, 2, setOf(10, 11)))
        val a22 = store2.create(RuleSessionRecord(null, 2, setOf(11, 12)))

        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
    }

    @Test
    fun `delete last added`() {
        val a11 = store1.create(RuleSessionRecord(null, 1, setOf(1)))
        val a21 = store2.create(RuleSessionRecord(null, 1, setOf(2)))
        val a12 = store1.create(RuleSessionRecord(null, 2, setOf(10, 11)))
        val a22 = store2.create(RuleSessionRecord(null, 2, setOf(11, 12)))
        val a13 = store1.create(RuleSessionRecord(null, 2, setOf(100, 110)))
        val a23 = store2.create(RuleSessionRecord(null, 2, setOf(110, 120)))

        store1.all() shouldBe setOf(a11, a12, a13)
        store2.all() shouldBe setOf(a21, a22, a23)
        store1.deleteLastAdded()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22, a23)
        store2.deleteLastAdded()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
        reload()
        store1.all() shouldBe setOf(a11, a12)
        store2.all() shouldBe setOf(a21, a22)
    }

    @Test
    fun load()
    {
        val a11 = RuleSessionRecord(10, 1, setOf(1))
        val a21 = RuleSessionRecord(10, 1, setOf(2))
        val a12 = RuleSessionRecord(20, 2, setOf(10, 11))
        val a22 = RuleSessionRecord(20, 2, setOf(11, 12))
        val a13 = RuleSessionRecord(30, 2, setOf(100, 110))
        val a23 = RuleSessionRecord(30, 2, setOf(110, 120))
        store1.load(setOf(a11, a12, a13))
        store2.load(setOf(a21, a22, a23))

        store1.all() shouldBe setOf(a11, a12, a13)
        store2.all() shouldBe setOf(a21, a22, a23)
        reload()
        store1.all() shouldBe setOf(a11, a12, a13)
        store2.all() shouldBe setOf(a21, a22, a23)
    }

    @Test
    fun deleteImpl() {
        val a11 = store1.create(RuleSessionRecord(null, 1, setOf(1)))
        val a21 = store2.create(RuleSessionRecord(null, 1, setOf(2)))
        val a12 = store1.create(RuleSessionRecord(null, 2, setOf(10, 11)))
        val a22 = store2.create(RuleSessionRecord(null, 2, setOf(11, 12)))
        val a13 = store1.create(RuleSessionRecord(null, 2, setOf(100, 110)))
        val a23 = store2.create(RuleSessionRecord(null, 2, setOf(110, 120)))

        store1.all() shouldBe setOf(a11, a12, a13)
        store2.all() shouldBe setOf(a21, a22, a23)
        store1.deleteImpl(a11)
        store1.all() shouldBe setOf(a12, a13)
        store2.all() shouldBe setOf(a21, a22, a23)
        store2.deleteImpl(a22)
        store1.all() shouldBe setOf(a12, a13)
        store2.all() shouldBe setOf(a21, a23)
        reload()
        store1.all() shouldBe setOf(a12, a13)
        store2.all() shouldBe setOf(a21, a23)
    }

    @Test
    fun createImpl()
    {
        val a11 = RuleSessionRecord(10, 1, setOf(1))
        val a21 = RuleSessionRecord(10, 1, setOf(2))
        val a12 = RuleSessionRecord(20, 2, setOf(10, 11))
        val a22 = RuleSessionRecord(20, 2, setOf(11, 12))

        val s11 = store1.createImpl(a11)
        val s21 = store2.createImpl(a21)
        val s12 = store1.createImpl(a12)
        val s22 = store2.createImpl(a22)

        store1.all() shouldBe setOf(s11, s12)
        store2.all() shouldBe setOf(s21, s22)
        reload()
        store1.all() shouldBe setOf(s11, s12)
        store2.all() shouldBe setOf(s21, s22)
    }
}