package io.rippledown.model

import kotlinx.serialization.Serializable

/**
 * The name of a user-defined case list and the ids of its cases,
 * in the order in which they were added to the list.
 */
@Serializable
data class CaseListInfo(val name: String, val caseIds: List<CaseId> = listOf())

@Serializable
data class CasesInfo(
    val caseIds: List<CaseId> = listOf(),
    val cornerstoneCaseIds: List<CaseId> = listOf(),
    val userDefinedCaseLists: List<CaseListInfo> = listOf(),
    val kbName: String = ""
) {
    val count get() = caseIds.size + cornerstoneCaseIds.size + userDefinedCaseLists.sumOf { it.caseIds.size }
}
