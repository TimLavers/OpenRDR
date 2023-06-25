package io.rippledown.kb

import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase

class CaseManager {
    private val cases = mutableListOf<RDRCase>()

    fun getCase(id: Long): RDRCase? {
        return cases.firstOrNull { it.caseId.id == id }
    }

    fun ids() = all().map { it.caseId }

    fun all(): List<RDRCase> {
        return cases
    }

    fun delete(id: Long) {
        cases.removeIf{it.id == id}
    }

    fun add(case: RDRCase): RDRCase {
        require(case.caseId.id == null) {"Cannot add a case that already has an id."}
        val newId = if (cases.isEmpty()) 1  else cases.maxOfOrNull { it.caseId.id!! }!! + 1
        val caseToStore = RDRCase(CaseId(newId, case.name), case.data)
        cases.add(caseToStore)
        return caseToStore
    }

    fun load(data: List<RDRCase>) { // todo test
        data.forEach {
            requireNotNull(it.caseId.id) {
                "Cannot load a case with a null id."
            }
        }
        require(cases.isEmpty()) {
            "Cannot load if there are already cases."
        }
        cases.addAll(data)
    }
}