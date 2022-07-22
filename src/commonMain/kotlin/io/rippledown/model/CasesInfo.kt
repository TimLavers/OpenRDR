package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CasesInfo(val caseIds: List<CaseId>, val resourcePath: String) {
    val count get() = caseIds.size
}