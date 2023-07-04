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

        val caseId0 = store.put(case0).caseId
        val caseId1 = store.put(case1).caseId
        val caseId2 = store.put(case2).caseId

        store.allCaseIds() shouldBe listOf(caseId0, caseId1, caseId2)

        reload()
        store.allCaseIds() shouldBe listOf(caseId0, caseId1, caseId2)
    }

    @Test
    fun dataPointsCount() {
        store.dataPointsCount() shouldBe 0
        store.put(case0)
        store.dataPointsCount() shouldBe 2
        store.put(case2)
        store.dataPointsCount() shouldBe 5
    }

    @Test
    fun put() {
        val stored0 = store.put(case0)
        stored0.caseId.id shouldNotBe null
        stored0 shouldNotBeSameInstanceAs case0
        stored0.data shouldBe case0.data

        val retrieved0 = store.get(stored0.id!!, attributeProvider)!!
        retrieved0 shouldBe stored0
        retrieved0.data shouldBe stored0.data

        TODO("put more in and check return order, also check after restore")
    }

    @Test
    fun `create case with test result with units`() {
        val testResult = TestResult("4.5", null, "furlongs/fortnight")
        val builder = RDRCaseBuilder()
        builder.addResult(a, today, testResult)
        val unitsCase = builder.build("Units Case")
        val stored = store.put(unitsCase)
        with (store.get(stored.id!!, attributeProvider)!!) {
            values(a)!![0] shouldBe testResult
            data shouldBe stored.data
        }

        reload()
        with (store.get(stored.id!!, attributeProvider)!!) {
            values(a)!![0] shouldBe testResult
            data shouldBe stored.data
        }
    }

    @Test
    fun `put case with test result with reference range`() {
        val lowRangeResult = TestResult("1.0", ReferenceRange("0.5", null), null)
        val highRangeResult = TestResult("2.0", ReferenceRange(null, "2.0"), "mmol/L")
        val rangeResult = TestResult("3.0", ReferenceRange("1.0", "5.0"), "mmol/L")
        val builder = RDRCaseBuilder()
        builder.addResult(a, today, lowRangeResult)
        builder.addResult(b, today, highRangeResult)
        builder.addResult(c, today, rangeResult)

        val rangeCase = builder.build("Range Case")
        val stored = store.put(rangeCase)
        with (store.get(stored.id!!, attributeProvider)!!) {
            values(a)!![0] shouldBe lowRangeResult
            values(b)!![0] shouldBe highRangeResult
            values(c)!![0] shouldBe rangeResult
            data shouldBe stored.data
        }

        reload()
        with (store.get(stored.id!!, attributeProvider)!!) {
            values(a)!![0] shouldBe lowRangeResult
            values(b)!![0] shouldBe highRangeResult
            values(c)!![0] shouldBe rangeResult
            data shouldBe stored.data
        }
    }

    @Test
    fun get() {
        // Largely tested elsewhere.
        store.get(9, attributeProvider) shouldBe null
    }

    @Test
    fun delete() {
        val stored0 = store.put(case0)
        store.delete(stored0.id!!)
        store.get(stored0.id!!, attributeProvider) shouldBe null
        store.dataPointsCount() shouldBe 0
    }

    @Test
    fun deleteUnknownCase() {
        store.delete(9)
    }

    @Test
    fun `can store multiple versions of same case`() {
        val stored00 = store.put(case0)
        val stored01 = store.put(case0)

        stored00.id shouldNotBe stored01.id
        stored00.name shouldBe stored01.name
        stored00.data shouldBe stored01.data
    }

    @Test
    fun load() {
        val caseX = createCase("X", mapOf(a to "1", b to "1"), 10)
        val caseY = createCase("Y", mapOf(a to "2", b to "3"), 100)
        val caseZ = createCase("Z", mapOf(a to "5", b to "8", c to "88"), 1000)
        val caseList = listOf(caseX, caseY, caseZ)
        store.load(caseList)

        TODO("check, also test load with some there, load with null ids, also order after load, also restore")
    }

    private fun createCase(name: String, attributeToValue: Map<Attribute, String>, id: Long? = null): RDRCase {
        val builder = RDRCaseBuilder()
        attributeToValue.forEach { (attribute, value) -> builder.addResult(attribute, today, TestResult(value)) }
        return builder.build(name, id)
    }
}