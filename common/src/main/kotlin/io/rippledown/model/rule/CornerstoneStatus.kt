package io.rippledown.model.rule

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.DerivedValueChange
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.PendingChange
import kotlinx.serialization.Serializable

/**
 * The current cornerstone case to review, plus information on where the user is up to in the list of cornerstones.
 *
 * [pendingChange] is the change the rule session in progress is about to make,
 * previewed by whichever panel it belongs to: a [Diff] by the Comments panel, a
 * [DerivedValueChange] by the Derived attributes panel. A session makes one
 * change, so this is one field rather than one per panel.
 */
@Serializable
data class CornerstoneStatus(
    val cornerstoneToReview: ViewableCase? = null,
    val indexOfCornerstoneToReview: Int = -1,
    val numberOfCornerstones: Int = 0,
    val pendingChange: PendingChange? = null,
    val ruleConditions: List<String> = emptyList()
) {
    /**
     * The pending change to the case's comments, or null if the session in
     * progress is not changing them.
     */
    val commentDiff: Diff?
        get() = pendingChange as? Diff

    /**
     * The pending change to one of the case's derived attributes, or null if the
     * session in progress is not changing one.
     */
    val derivedValueDiff: DerivedValueChange?
        get() = pendingChange as? DerivedValueChange

    init {
        require(indexOfCornerstoneToReview < numberOfCornerstones) { "index of the cornerstone to show is between -1 and the number of cornerstones" }
    }

    fun summary() =
        "Cornerstone: ${cornerstoneToReview?.name}, Index: ${indexOfCornerstoneToReview}, Total: $numberOfCornerstones"
}
