package io.rippledown.kb

import io.kotest.matchers.shouldBe
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
        caseManager.all() shouldBe empty()
    }

    @Test
    fun add() {
        val expected = mutableSetOf<RDRCase>()
        repeat(100) {
            caseManager.all() shouldBe expected
            val case = makeCase("Case$it", "gl_$it", "ft4_$it")
            caseManager.add(case)
            expected.add(case)
        }
        caseManager.all() shouldBe expected
    }

    @Test
    fun `cannot add a case that already has an id`() {

    }

    private fun makeCase(name: String, glucoseValue: String = "3.2", ft4Value: String = "1.2"): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addResult(glucose, defaultTestDate, TestResult(glucoseValue))
        builder.addResult(ft4, defaultTestDate, TestResult(ft4Value))
        return builder.build(name)
    }
}