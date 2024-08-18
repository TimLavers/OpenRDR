package io.rippledown.model.rule

import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.RuleConditionList
import kotlinx.serialization.Serializable

/**
 * This is the information that is sent from the GUI tp the server to update the CornerstoneStatus when the conditions in the current rule building session change.
 *
 * @param cornerstoneStatus The current CornerstoneStatus
 * @param conditionList The updated list of conditions for the rule
 */
@Serializable
data class UpdateCornerstoneRequest(
    val cornerstoneStatus: CornerstoneStatus,
    val conditionList: RuleConditionList = RuleConditionList()
)