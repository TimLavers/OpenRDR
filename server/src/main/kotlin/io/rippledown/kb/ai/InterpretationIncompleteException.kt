package io.rippledown.kb.ai

import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation

/**
 * A case could not be interpreted. Carries the wire form, so that a route can
 * answer with it and the client can ask for the retained case again.
 */
class InterpretationIncompleteException(val details: IncompleteInterpretation) : RuntimeException(details.reason)

fun PreparedInterpretation.Incomplete.toException(caseId: Long?) = InterpretationIncompleteException(
    IncompleteInterpretation(caseId, IncompleteCategory.EVALUATION_FAILED, attribute.name, reason)
)

fun PreparedInterpretation.Abandoned.toException(caseId: Long?) = InterpretationIncompleteException(
    IncompleteInterpretation(caseId, IncompleteCategory.ABANDONED, null, reason)
)
