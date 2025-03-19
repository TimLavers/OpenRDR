package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.cornerstone.CornerstonePager
import io.rippledown.cornerstone.CornerstonePagerHandler
import io.rippledown.main.Handler
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

interface CaseControlHandler : Handler, CaseInspectionHandler, CornerstonePagerHandler {
    fun getCase(caseId: Long)
    fun startRuleSession(sessionStartRequest: SessionStartRequest)
    fun endRuleSession()
    fun buildRule(ruleRequest: RuleRequest)
    fun updateCornerstoneStatus(cornerstoneRequest: UpdateCornerstoneRequest)
    fun conditionForExpression(conditionText: String, attributeNames: Collection<String>): ConditionParsingResult
}

@Composable
fun CaseControl(
    currentCase: ViewableCase?,
    cornerstoneStatus: CornerstoneStatus? = null,
    conditionHints: List<SuggestedCondition>,
    handler: CaseControlHandler
) {
    val ruleInProgress = cornerstoneStatus != null
    val attributeNames = conditionHints.flatMap { it.initialSuggestion().attributeNames() }.toSet()

    Row(
        modifier = Modifier
            .padding(10.dp)
            .width(1800.dp)
    )
    {
        if (currentCase != null) {
            CaseInspection(currentCase, ruleInProgress, object : CaseInspectionHandler, Handler by handler {
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            })
        }
        if (ruleInProgress) {
            if (cornerstoneStatus!!.cornerstoneToReview == null) {
                handler.setRightInfoMessage(NO_CORNERSTONES_TO_REVIEW_MSG)
            } else {
                handler.setRightInfoMessage("")
                CornerstonePager(cornerstoneStatus, handler)
            }

            Spacer(modifier = Modifier.width(5.dp))
            RuleMaker(conditionHints, object : RuleMakerHandler, Handler by handler {
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
                    handler.conditionForExpression(expression, attributeNames)
            })
        }
    }
}