package io.rippledown.kb

import io.rippledown.model.CaseListInfo
import io.rippledown.model.CaseListType
import io.rippledown.model.RDRCase
import io.rippledown.persistence.CaseStore

class CaseManager(private val caseStore: CaseStore, private val attributeManager: AttributeManager) {

    fun getCase(id: Long) = caseStore.get(id, attributeManager)

    fun ids() = caseStore.allCaseIds()

    fun ids(type: CaseListType) = ids().filter { it.type == type }

    fun all() = caseStore.all(attributeManager)

    fun all(type: CaseListType) = all().filter { it.caseId.type == type }

    fun userDefinedCaseLists(): List<CaseListInfo> = ids()
        .filter { !it.type.isBuiltIn }
        .groupBy { it.type }
        .values
        .map { idsForList -> idsForList.sortedBy { it.id ?: Long.MAX_VALUE } }
        .map { ordered -> CaseListInfo(ordered.first().type.name, ordered) }
        .sortedBy { it.caseIds.first().id ?: Long.MAX_VALUE }

    fun delete(id: Long) {
        caseStore.delete(id)
    }

    fun add(case: RDRCase): RDRCase {
        require(case.caseId.id == null) {"Cannot add a case that already has an id."}
        return caseStore.put(case)
    }

    fun load(data: List<RDRCase>) = caseStore.load(data)
}