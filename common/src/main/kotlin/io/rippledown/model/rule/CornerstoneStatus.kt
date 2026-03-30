package io.rippledown.model.rule

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff
import kotlinx.serialization.Serializable

/**
 * The current cornerstone case to review, plus information on where the user is up to in the list of cornerstones.
 */
@Serializable
data class CornerstoneStatus(
    val cornerstoneToReview: ViewableCase? = null,
    val indexOfCornerstoneToReview: Int = -1,
    val numberOfCornerstones: Int = 0,
    val diff: Diff? = null
) {
    init {
        require(indexOfCornerstoneToReview < numberOfCornerstones) { "index of the cornerstone to show is between -1 and the number of cornerstones" }
    }

    fun summary() =
        "Cornerstone: ${cornerstoneToReview?.name}, Index: ${indexOfCornerstoneToReview}, Total: $numberOfCornerstones"
}
