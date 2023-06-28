package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.persistence.CaseStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresCaseStoreTest: PostgresStoreTest() {
    private val a = Attribute(1000, "A")
    private val b = Attribute(1001, "B")
    private val c = Attribute(1002, "C")
    private val case0 = createCase("Tea Case", mapOf(a to "1", b to "1"))
    private val case1 = createCase("Coffee Case", mapOf(a to "2", b to "3"))
    private val case2 = createCase("Beer Case", mapOf(a to "5", b to "8"))
    private lateinit var store: CaseStore
    override fun tablesInDropOrder() = listOf("cases_processed")

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.caseStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.caseStore()
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

    /*
    - store new case
    - update case by changing name
    - update case by changing a test result value
    - update case by changing a test result units
    - update case by changing a test result reference range
    - update case by adding a new test result
    - update case by removing a test result
     */


    private fun createCase(name: String, attributeToValue: Map<Attribute, String>): RDRCase {
        val builder = RDRCaseBuilder()
        attributeToValue.forEach { (attribute, value) -> builder.addResult(attribute, today, TestResult(value)) }
        return builder.build(name)
    }
}