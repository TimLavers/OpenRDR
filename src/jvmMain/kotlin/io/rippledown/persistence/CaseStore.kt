package io.rippledown.persistence

import io.rippledown.kb.AttributeProvider
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase

interface CaseStore {
    fun allCaseIds(): List<CaseId>
    fun all(attributeProvider: AttributeProvider): List<RDRCase>
    fun put(case: RDRCase): RDRCase
    fun load(cases: List<RDRCase>)
    fun get(id: Long, attributeProvider: AttributeProvider): RDRCase?
    fun delete(id: Long)
}