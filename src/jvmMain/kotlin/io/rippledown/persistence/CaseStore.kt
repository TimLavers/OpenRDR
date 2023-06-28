package io.rippledown.persistence

import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase

interface CaseStore {
    fun allCaseIds(): List<CaseId>
    fun all(): List<RDRCase>
    fun create(case: RDRCase): RDRCase
    fun update(case: RDRCase)
    fun load(cases: List<RDRCase>)
    fun get(id: Long): RDRCase?
    fun delete(id: Long): Boolean
}