package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.cornerstone.CornerstonePager
import io.rippledown.cornerstone.CornerstonePagerHandler
import io.rippledown.main.Handler
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.Diff
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.rule.RuleMaker
import io.rippledown.rule.RuleMakerHandler
import kotlinx.coroutines.delay

interface CaseControlHandler : Handler, CaseInspectionHandler, CornerstonePagerHandler {
    fun getCase(caseId: Long)
    fun saveCase(case: ViewableCase)
    fun startRuleSession(sessionStartRequest: SessionStartRequest)
    fun endRuleSession()
    fun buildRule(ruleRequest: RuleRequest)
    fun updateCornerstoneStatus(cornerstoneRequest: UpdateCornerstoneRequest)
}

@Composable
@Preview
fun CaseControl(
    currentCase: ViewableCase?,
    cornerstoneStatus: CornerstoneStatus? = null,
    conditionHints: List<Condition>,
    handler: CaseControlHandler
) {
    var verifiedText: String? by remember { mutableStateOf(null) }
    val indexOfSelectedDiff: Int by remember { mutableStateOf(-1) }

    val ruleInProgress = cornerstoneStatus != null

    LaunchedEffect(verifiedText, indexOfSelectedDiff) {
        if (verifiedText != null || indexOfSelectedDiff != -1) {
            delay(DEBOUNCE_WAIT_PERIOD_MILLIS)
            val updatedCase = currentCase!!.copy(
                viewableInterpretation = currentCase.viewableInterpretation
                    .copy(verifiedText = verifiedText)
            )
            handler.saveCase(updatedCase)
        }
    }

    Row(
        modifier = Modifier
            .padding(10.dp)
            .width(1800.dp)
    )
    {

        if (currentCase != null) {
            CaseInspection(currentCase, ruleInProgress, object : CaseInspectionHandler, Handler by handler {
                override fun onStartRule(selectedDiff: Diff) {
                    handler.startRuleSession(SessionStartRequest(currentCase.id!!, selectedDiff))
                }

                override var onInterpretationEdited: (text: String) -> Unit = {
                    verifiedText = it

                }
                override var isCornerstone: Boolean = false
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            })
        }
        if (ruleInProgress) {
            if (cornerstoneStatus!!.cornerstoneToReview == null) {
                handler.setInfoMessage(NO_CORNERSTONES_TO_REVIEW_MSG)
            } else {
                handler.setInfoMessage("")
                CornerstonePager(cornerstoneStatus, object : CornerstonePagerHandler by handler {
                    override fun exemptCornerstone(index: Int) {
                        handler.exemptCornerstone(index)
                    }
                })
            }

            Spacer(modifier = Modifier.width(5.dp))
            RuleMaker(conditionHints, object : RuleMakerHandler, Handler by handler {
                override var onDone = { conditions: List<Condition> ->
                    val ruleRequest = RuleRequest(currentCase!!.id!!, ConditionList(conditions))
                    handler.buildRule(ruleRequest)
                }

                override var onCancel = { handler.endRuleSession() }

                override var onUpdateConditions = { conditions: List<Condition> ->
                    val ccUpdateRequest = UpdateCornerstoneRequest(cornerstoneStatus, ConditionList(conditions))
                    handler.updateCornerstoneStatus(ccUpdateRequest)
                }
            })
        }
    }
}