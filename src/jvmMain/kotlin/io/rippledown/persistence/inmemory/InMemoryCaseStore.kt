package io.rippledown.persistence.inmemory

import io.rippledown.kb.AttributeProvider
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.persistence.CaseStore

class InMemoryCaseStore: CaseStore {
    private val data = mutableListOf<RDRCase>()

    override fun all() = data.toList()

    override fun allCaseIds() = all().map { it.caseId }

    override fun create(case: RDRCase): RDRCase {
        require (case.id == null) {
            "The case has an id already, please use update instead."
        }
        val newId = if (data.isEmpty()) 1  else data.maxOfOrNull { it.caseId.id!! }!! + 1
        val caseToStore = RDRCase(CaseId(newId, case.name), case.data)
        data.add(caseToStore)
        return caseToStore
    }

    override fun load(cases: List<RDRCase>) {
        cases.forEach {
            requireNotNull(it.caseId.id) {
                "Cannot load a case with a null id."
            }
        }
        require(data.isEmpty()) {
            "Cannot load if there are already cases."
        }
        data.addAll(data)
    }

    override fun get(id: Long, attributeProvider: AttributeProvider) = data.firstOrNull { id == it.id }

    override fun delete(id: Long) = data.removeIf{it.id == id}
}