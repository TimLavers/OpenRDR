package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.*
import io.rippledown.persistence.CaseStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryCaseStore
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class CaseManagerTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var glucose: Attribute
    private lateinit var ft4: Attribute
    private lateinit var caseStore: CaseStore
    private lateinit var caseManager: CaseManager

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        glucose = attributeManager.getOrCreate("Glucose")
        ft4 = attributeManager.getOrCreate("FT4")
        caseStore = InMemoryCaseStore()
        caseManager = CaseManager(caseStore, attributeManager)
    }

    @Test
    fun construction() {
        val caseA = makeCase("Case A", "4.1", "nil")
        val idA = caseManager.add(caseA).caseId
        val caseB = makeCase("Case B", "4.0", "nil")
        val idB = caseManager.add(caseB).caseId

        val new = CaseManager(caseStore, attributeManager)
        new.ids() shouldBe listOf(idA, idB)
        new.getCase(idA.id!!)!!.caseId shouldBe idA
        new.getCase(idB.id!!)!!.caseId shouldBe idB
    }

    @Test
    fun empty() {
        caseManager.all() shouldBe emptyList()
    }

    @Test
    fun add() {
        val expected = mutableListOf<RDRCase>()
        repeat(100) {
            caseManager.all() shouldBe expected
            val case = makeCase("Case$it", "gl_$it", "ft4_$it")
            val stored = caseManager.add(case)
            expected.add(stored)
            stored.caseId.name shouldBe case.caseId.name
            stored.caseId.id shouldNotBe null
            stored.data shouldBe case.data

            val retrieved = caseManager.getCase(stored.caseId.id!!)!!
            retrieved.caseId.name shouldBe case.caseId.name
            retrieved.caseId.id shouldNotBe null
            retrieved.data shouldBe case.data
        }
        caseManager.all() shouldBe expected
    }

    @Test
    fun ids() {
        caseManager.ids() shouldBe emptyList()

        val caseA = makeCase("Case A", "4.1", "nil")
        val idA = caseManager.add(caseA).caseId
        caseManager.ids() shouldBe listOf(idA)

        val caseB = makeCase("Case B", "4.0", "nil")
        val idB = caseManager.add(caseB).caseId
        caseManager.ids() shouldBe listOf(idA, idB)
    }

    @Test
    fun `cannot add a case that already has an id`() {
        val case = makeCase("A")
        val withId = RDRCase(CaseId(89, "A"), case.data)
        shouldThrow<IllegalArgumentException> {
            caseManager.add(withId)
        }.message shouldBe "Cannot add a case that already has an id."
    }

    @Test
    fun getCase() {
        // Mostly tested in add() test.
        caseManager.getCase(77) shouldBe null
    }

    @Test
    fun `delete a case with an unknown id does nothing`() {
        caseManager.delete(9)
    }

    @Test
    fun delete() {
        val caseA = makeCase("Case A", "4.1", "nil")
        val idA = caseManager.add(caseA).caseId
        val caseB = makeCase("Case B", "4.0", "nil")
        val idB = caseManager.add(caseB).caseId
        caseManager.ids() shouldBe listOf(idA, idB) // sanity

        caseManager.delete(idA.id!!)
        caseManager.ids() shouldBe listOf(idB)
        caseManager.getCase(idB.id!!)!!.name shouldBe idB.name
        caseManager.getCase(idA.id!!) shouldBe null
    }

    private fun makeCase(name: String, glucoseValue: String = "3.2", ft4Value: String = "1.2"): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addResult(glucose, defaultDate, TestResult(glucoseValue))
        builder.addResult(ft4, defaultDate, TestResult(ft4Value))
        return builder.build(name)
    }
}