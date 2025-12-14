package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.cornerstone.CornerstonePager
import io.rippledown.cornerstone.CornerstonePagerHandler
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.rule.RuleMaker
import io.rippledown.rule.RuleMakerHandler

interface CaseControlHandler : CaseInspectionHandler, CornerstonePagerHandler {
    fun getCase(caseId: Long)
    fun startRuleSession(sessionStartRequest: SessionStartRequest)
    fun endRuleSession()
    fun buildRule(ruleRequest: RuleRequest)
    fun updateCornerstoneStatus(cornerstoneRequest: UpdateCornerstoneRequest)
    fun conditionFor(conditionText: String): ConditionParsingResult
    var setRightInfoMessage: (message: String) -> Unit
}

@Composable
fun CaseControl(
    currentCase: ViewableCase?,
    cornerstoneStatus: CornerstoneStatus? = null,
    isChatVisible: Boolean = false,
    conditionHints: List<SuggestedCondition>,
    handler: CaseControlHandler,
    modifier: Modifier = Modifier
) {
    val ruleInProgress = cornerstoneStatus != null

    Row(
        modifier = modifier
            .padding(8.dp)
    )
    {
        if (currentCase != null) {
            val showChangeInterpretationIcon = !ruleInProgress && !isChatVisible
            CaseInspection(currentCase, showChangeInterpretationIcon, object : CaseInspectionHandler by handler {
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            })
        }
        if (ruleInProgress) {
            if (cornerstoneStatus.cornerstoneToReview == null) {
                handler.setRightInfoMessage(NO_CORNERSTONES_TO_REVIEW_MSG)
            } else {
                handler.setRightInfoMessage("")
                CornerstonePager(cornerstoneStatus, handler)
            }

            Spacer(modifier = Modifier.width(5.dp))
            RuleMaker(conditionHints, object : RuleMakerHandler {

                override var onDone = { conditions: List<Condition> ->
                    val ruleRequest = RuleRequest(currentCase!!.id!!, RuleConditionList(conditions))
                    handler.buildRule(ruleRequest)
                }

                override var onCancel = {
                    handler.endRuleSession()
                }

                override var onUpdateConditions = { conditions: List<Condition> ->
                    val ccUpdateRequest = UpdateCornerstoneRequest(cornerstoneStatus, RuleConditionList(conditions))
                    handler.updateCornerstoneStatus(ccUpdateRequest)
                }

                override fun conditionForExpression(expression: String) =
                    handler.conditionFor(expression)
            })
        }
    }
}