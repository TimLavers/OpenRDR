package io.rippledown.model.caseview

import kotlinx.serialization.Serializable
import io.rippledown.model.RDRCase

enum class IncompleteCategory {
    /** An AI evaluation the interpretation depends on did not succeed. */
    EVALUATION_FAILED,

    /** The interpretation could not be published: the case was deleted or the KB kept changing. */
    ABANDONED
}

/**
 * Why a case could not be interpreted. The case id is that of the stored
 * case, so that the caller can ask for it again rather than resubmit it.
 * See documentation/design/ai_derived_attributes.md.
 */
@Serializable
data class IncompleteInterpretation(
    val caseId: Long?,
    val category: IncompleteCategory,
    val attributeName: String?,
    val reason: String,
    val primaryCase: RDRCase? = null,
    val caseViewProperties: CaseViewProperties? = null
) {
    /** A failed view still displays the primary data, never a partial interpretation. */
    fun caseView(): ViewableCase? = primaryCase?.let { case ->
        ViewableCase(case, caseViewProperties ?: CaseViewProperties(case.attributes.toList()),
            incomplete = copy(primaryCase = null, caseViewProperties = null))
    }
}
