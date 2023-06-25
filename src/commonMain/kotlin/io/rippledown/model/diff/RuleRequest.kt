package io.rippledown.model.diff

import io.rippledown.model.condition.ConditionList
import kotlinx.serialization.Serializable

/**
 * This is the information that is sent from the GUI tp the server to build a rule.
 */
@Serializable
data class RuleRequest(
    val caseId: Long,
    val diffList: DiffList = DiffList(),
    val conditionList: ConditionList = ConditionList()
) {
    init {
        require(diffList.selected > -1) { "a diff must be selected in order to build a rule" }
    }
}
