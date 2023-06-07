package io.rippledown.model.rule

import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.DiffList
import kotlinx.serialization.Serializable

/**
 * This is the information that is sent from the GUI tp the server to build a rule.
 */
@Serializable
data class RuleRequest(
    val caseId: String = "",
    val diffList: DiffList = DiffList(),
    val conditionList: ConditionList = ConditionList()
) {
    init {
        require(diffList.selected > -1) { "a diff must be selected in order to build a rule" }
    }
}
