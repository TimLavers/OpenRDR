package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CasesInfo(val caseIds: List<CaseId> = listOf(), val kbName: String = "") {
    val count get() = caseIds.size
}