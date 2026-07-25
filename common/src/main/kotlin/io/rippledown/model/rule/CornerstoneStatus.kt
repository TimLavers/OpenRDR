package io.rippledown.model.rule

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.DerivedValueChange
import io.rippledown.model.diff.Diff
import kotlinx.serialization.Serializable

/**
 * The current cornerstone case to review, plus information on where the user is up to in the list of cornerstones.
 *
 * [diff] describes a pending change to the case's comments, and
 * [derivedValueChange] a pending change to one of its derived attributes. They
 * are separate so that each panel is given only the preview that belongs to it.
 */
@Serializable
data class CornerstoneStatus(
    val cornerstoneToReview: ViewableCase? = null,
    val indexOfCornerstoneToReview: Int = -1,
    val numberOfCornerstones: Int = 0,
    val diff: Diff? = null,
    val ruleConditions: List<String> = emptyList(),
    val derivedValueChange: DerivedValueChange? = null
) {
    init {
        require(indexOfCornerstoneToReview < numberOfCornerstones) { "index of the cornerstone to show is between -1 and the number of cornerstones" }
    }

    fun summary() =
        "Cornerstone: ${cornerstoneToReview?.name}, Index: ${indexOfCornerstoneToReview}, Total: $numberOfCornerstones"
}
