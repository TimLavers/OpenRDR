package io.rippledown.model.rule

import io.rippledown.model.condition.ConditionList
import kotlinx.serialization.Serializable

/**
 * The information that is sent from the GUI to the server to build a rule.
 * Note that the current interpretation of the case on the server already identifies the selected Diff to build the rule on.
 */
@Serializable
data class RuleRequest(
    val caseId: Long,
    val conditionList: ConditionList = ConditionList()
)