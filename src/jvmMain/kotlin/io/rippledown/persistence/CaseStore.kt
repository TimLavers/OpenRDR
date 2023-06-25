package io.rippledown.persistence

import io.rippledown.model.RDRCase

interface CaseStore {
    fun all(): Set<RDRCase>
    fun store(case: RDRCase)
    fun load(cases: Set<RDRCase>)
}