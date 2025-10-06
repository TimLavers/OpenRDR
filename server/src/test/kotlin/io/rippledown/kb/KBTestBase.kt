package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

open class KBTestBase {
    val kbInfo = KBInfo("id123", "MyKB")
    lateinit var kb: KB

    open fun setup() {
        kb = KB(InMemoryKB(kbInfo))
    }

    fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    fun createCase(
        caseName: String,
        attribute: Attribute = glucose(),
        value: String = "0.667",
        range: ReferenceRange? = null,
        id: Long? = null
    ): ViewableCase {
        with(RDRCaseBuilder()) {
            val testResult = TestResult(value, range)
            addResult(attribute, defaultDate, testResult)
            val case = build(caseName, id)
            val caseViewProperties = CaseViewProperties(listOf(attribute))
            return ViewableCase(case, caseViewProperties)
        }
    }
}