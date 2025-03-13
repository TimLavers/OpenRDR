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
}