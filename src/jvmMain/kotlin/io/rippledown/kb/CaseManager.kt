package io.rippledown.kb

import io.rippledown.model.CaseType
import io.rippledown.model.RDRCase
import io.rippledown.persistence.CaseStore

class CaseManager(private val caseStore: CaseStore) {

    fun getCase(id: Long) = caseStore.get(id)

    fun ids() = all().map { it.caseId }
    fun ids(type: CaseType) = ids().filter { it.type == type }

    fun all() = caseStore.all()
    fun all(type: CaseType) = all().filter { it.caseId.type == type }

    fun delete(id: Long) {
        caseStore.delete(id)
    }

    fun add(case: RDRCase): RDRCase {
        require(case.caseId.id == null) {"Cannot add a case that already has an id."}
        return caseStore.create(case)
    }

    fun load(data: List<RDRCase>) = caseStore.load(data)
}