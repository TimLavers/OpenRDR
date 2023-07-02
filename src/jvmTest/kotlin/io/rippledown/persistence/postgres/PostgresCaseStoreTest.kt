package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.kb.AttributeProvider
import io.rippledown.model.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresCaseStoreTest: PostgresStoreTest() {
    private val a = Attribute(1000, "A")
    private val b = Attribute(1001, "B")
    private val c = Attribute(1002, "C")
    private val idToAttribute = mapOf(a.id to a, b.id to b, c.id to c)
    private val attributeProvider = AttributeProvider{idToAttribute[it]!!}
    private val case0 = createCase("Tea Case", mapOf(a to "1", b to "1"))
    private val case1 = createCase("Coffee Case", mapOf(a to "2", b to "3"))
    private val case2 = createCase("Beer Case", mapOf(a to "5", b to "8", c to "88"))
    private lateinit var store: PostgresCaseStore
    override fun tablesInDropOrder() = listOf("cases_processed")

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.caseStore() as PostgresCaseStore
    }

    override fun reload() {
        super.reload()
        store = postgresKB.caseStore() as PostgresCaseStore
    }

    @Test
    fun allCaseIds() {
        store.allCaseIds() shouldBe emptyList()

        val caseId0 = store.create(case0).caseId
        val caseId1 = store.create(case1).caseId
        val caseId2 = store.create(case2).caseId

        store.allCaseIds() shouldBe listOf(caseId0, caseId1, caseId2)

        reload()
        store.allCaseIds() shouldBe listOf(caseId0, caseId1, caseId2)
    }

    @Test
    fun dataPointsCount() {
        store.dataPointsCount() shouldBe 0
        store.create(case0)
        store.dataPointsCount() shouldBe 2
        store.create(case2)
        store.dataPointsCount() shouldBe 5
    }

    @Test
    fun create() {
        val stored0 = store.create(case0)
        stored0.caseId.id shouldNotBe null
        stored0 shouldNotBeSameInstanceAs case0
        stored0.data shouldBe case0.data

        val retrieved0 = store.get(stored0.id!!, attributeProvider)!!
        retrieved0 shouldBe stored0
        retrieved0.data shouldBe stored0.data
    }

    @Test
    fun get() {
        // Largely tested elsewhere.
        store.get(9, attributeProvider) shouldBe null
    }

    @Test
    fun delete() {
        val stored0 = store.create(case0)
        val b = store.delete(stored0.id!!)
        b shouldBe true
        store.get(stored0.id!!, attributeProvider) shouldBe null
        store.dataPointsCount() shouldBe 0
    }

    @Test
    fun deleteUnknownCase() {
        store.delete(9) shouldBe false
    }

    @Test
    fun `can store multiple versions of same case`() {
        val stored00 = store.create(case0)
        val stored01 = store.create(case0)

        stored00.id shouldNotBe stored01.id
        stored00.name shouldBe stored01.name
        stored00.data shouldBe stored01.data
    }

    private fun createCase(name: String, attributeToValue: Map<Attribute, String>): RDRCase {
        val builder = RDRCaseBuilder()
        attributeToValue.forEach { (attribute, value) -> builder.addResult(attribute, today, TestResult(value)) }
        return builder.build(name)
    }
}