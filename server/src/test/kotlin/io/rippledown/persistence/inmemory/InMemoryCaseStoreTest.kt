package io.rippledown.persistence.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.kb.AttributeProvider
import io.rippledown.model.*
import io.rippledown.persistence.createCase
import io.rippledown.utils.today
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryCaseStoreTest {
    private lateinit var store: InMemoryCaseStore
    private val a = Attribute(1000, "A")
    private val b = Attribute(1001, "B")
    private val c = Attribute(1002, "C")
    private val attributeProvider = object : AttributeProvider {
        override fun getById(id: Int) = TODO()

        override fun getOrCreate(text: String) = TODO()
    }
    private val case0 = createCase("Tea Case", mapOf(a to "1", b to "1"))
    private val case1 = createCase("Coffee Case", mapOf(a to "2", b to "3"))
    private val case2 = createCase("Beer Case", mapOf(a to "5", b to "8", c to "88"))

    @BeforeTest
    fun setup() {
        store = InMemoryCaseStore()
    }

    @Test
    fun `initially empty`() {
        store.allCaseIds() shouldBe emptyList()
    }

    @Test
    fun allCaseIds() {
        store.allCaseIds() shouldBe emptyList()

        val caseId0 = store.put(case0).caseId
        val caseId1 = store.put(case1).caseId
        val caseId2 = store.put(case2).caseId

        store.allCaseIds() shouldBe listOf(caseId0, caseId1, caseId2)
    }

    @Test
    fun caseTypes() {
        val caseId0 = store.put(case0.copyWithoutId(CaseType.Cornerstone)).caseId
        val caseId1 = store.put(case1).caseId
        val caseId2 = store.put(case2.copyWithoutId(CaseType.Cornerstone)).caseId

        store.allCaseIds() shouldBe listOf(caseId0, caseId1, caseId2)
        caseId0.type shouldBe CaseType.Cornerstone
        caseId1.type shouldBe CaseType.Processed
        caseId2.type shouldBe CaseType.Cornerstone
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

        val stored1 = store.put(case0)
        stored1.caseId.id shouldNotBe null
        stored1 shouldNotBeSameInstanceAs case0
        stored1.data shouldBe case0.data

        val retrieved1 = store.get(stored1.id!!, attributeProvider)!!
        retrieved1 shouldBe stored1
        retrieved1.data shouldBe stored1.data

        val stored2 = store.put(case0)
        stored2.caseId.id shouldNotBe null
        stored2 shouldNotBeSameInstanceAs case0
        stored2.data shouldBe case0.data

        val retrieved2 = store.get(stored2.id!!, attributeProvider)!!
        retrieved2 shouldBe stored2
        retrieved2.data shouldBe stored2.data

        store.allCaseIds().shouldBe(listOf(retrieved0.caseId, retrieved1.caseId, retrieved2.caseId))
        store.all(attributeProvider).shouldBe(listOf(retrieved0, retrieved1, retrieved2))
    }

    @Test
    fun `create case with test result with units`() {
        val testResult = TestResult("4.5", null, "furlongs/fortnight")
        val builder = RDRCaseBuilder()
        builder.addResult(a, today, testResult)
        val unitsCase = builder.build("Units Case")
        val stored = store.put(unitsCase)
        with(store.get(stored.id!!, attributeProvider)!!) {
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
        with(store.get(stored.id!!, attributeProvider)!!) {
            values(a)!![0] shouldBe lowRangeResult
            values(b)!![0] shouldBe highRangeResult
            values(c)!![0] shouldBe rangeResult
            data shouldBe stored.data
        }
    }

    @Test
    fun getUnknownCase() {
        store.get(9, attributeProvider) shouldBe null
    }

    @Test
    fun getReturnsCaseCopy() {
        val stored0 = store.put(case0)
        stored0.caseId.id shouldNotBe null
        stored0 shouldNotBeSameInstanceAs case0
        stored0.data shouldBe case0.data

        val retrieved0 = store.get(stored0.id!!, attributeProvider)!!
        retrieved0 shouldBe stored0
        retrieved0.data shouldBe stored0.data
        retrieved0.interpretation shouldNotBeSameInstanceAs  stored0.interpretation

        val retrieved1 = store.get(stored0.id!!, attributeProvider)!!
        retrieved1 shouldBe stored0
        retrieved1.data shouldBe stored0.data
        retrieved1.interpretation shouldNotBeSameInstanceAs stored0.interpretation
        retrieved1.interpretation shouldNotBeSameInstanceAs retrieved0.interpretation
    }

    @Test
    fun delete() {
        val stored0 = store.put(case0)
        store.delete(stored0.id!!)
        store.get(stored0.id!!, attributeProvider) shouldBe null
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

        store.allCaseIds() shouldBe listOf(caseX.caseId, caseY.caseId, caseZ.caseId)
        store.all(attributeProvider) shouldBe listOf(caseX, caseY, caseZ)
    }

    @Test
    fun `cannot load if not empty`() {
        val stored0 = store.put(case0)
        val caseX = createCase("X", mapOf(a to "1", b to "1"), 10)
        shouldThrow<IllegalArgumentException> {
            store.load(listOf(caseX))
        }.message shouldBe "Cannot load if there are already cases."
        store.all(attributeProvider) shouldBe listOf(stored0)
    }

    @Test
    fun `cannot load cases with non-null ids`() {
        shouldThrow<IllegalArgumentException> {
            store.load(listOf(case0))
        }.message shouldBe "Cannot load a case with a null id."
        store.all(attributeProvider) shouldBe emptyList()
    }
}