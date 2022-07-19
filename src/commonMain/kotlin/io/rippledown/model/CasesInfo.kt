package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CaseId(val id: String, val name: String)

@Serializable
data class CasesInfo(val caseIds: List<CaseId>, val resourcePath: String) {
    val count get() = caseIds.size
}