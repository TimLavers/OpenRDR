package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate

open class KBTestBase {
    val kbInfo = KBInfo("id123", "MyKB")
    lateinit var kb: KB
    lateinit var session: KBSession

    open fun setup() {
        kb = KB(InMemoryKB(kbInfo))
        session = KBSession(kb)
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
            val Result = Result(value, range)
            addResult(attribute, defaultDate, Result)
            val case = build(caseName, id)
            val caseViewProperties = CaseViewProperties(listOf(attribute))
            return ViewableCase(case, caseViewProperties)
        }
    }
}