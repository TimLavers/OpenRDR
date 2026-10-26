package io.rippledown.model.caseview

import kotlinx.serialization.Serializable

enum class IncompleteCategory {
    /** An AI evaluation the interpretation depends on did not succeed. */
    EVALUATION_FAILED,

    /** The interpretation could not be published: the case was deleted or the KB kept changing. */
    ABANDONED
}

/**
 * Why a case could not be interpreted. The case id is that of the stored
 * case, so that the caller can ask for it again rather than resubmit it.
 * See documentation/tickets/ai_derived_attributes_design.md.
 */
@Serializable
data class IncompleteInterpretation(
    val caseId: Long?,
    val category: IncompleteCategory,
    val attributeName: String?,
    val reason: String
)
