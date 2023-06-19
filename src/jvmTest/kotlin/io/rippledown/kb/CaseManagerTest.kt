package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.*
import io.rippledown.persistence.InMemoryAttributeStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class CaseManagerTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var glucose: Attribute
    private lateinit var ft4: Attribute
    private lateinit var caseManager: CaseManager

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        glucose = attributeManager.getOrCreate("Glucose")
        ft4 = attributeManager.getOrCreate("FT4")
        caseManager = CaseManager()
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

    private fun makeCase(name: String, glucoseValue: String = "3.2", ft4Value: String = "1.2"): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addResult(glucose, defaultTestDate, TestResult(glucoseValue))
        builder.addResult(ft4, defaultTestDate, TestResult(ft4Value))
        return builder.build(name)
    }
}