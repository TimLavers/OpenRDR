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
import io.rippledown.model.CasesInfo
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
    suspend fun conditionHintsForCase(caseId: Long): List<Condition>
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
    casesInfo: CasesInfo,
    handler: CaseControlHandler
) {
    var currentCaseId: Long? by remember { mutableStateOf(null) }
    var verifiedText: String? by remember { mutableStateOf(null) }
    val indexOfSelectedDiff: Int by remember { mutableStateOf(-1) }
    var conditionHintsForCase by remember { mutableStateOf(listOf<Condition>()) }

    val ruleInProgress = cornerstoneStatus != null

    LaunchedEffect(casesInfo, currentCaseId) {
        if (casesInfo.caseIds.isNotEmpty()) {
            if (currentCaseId == null || currentCaseId !in casesInfo.caseIds.map { it.id }) {
                //No initial case, or it's now been deleted
                currentCaseId = casesInfo.caseIds[0].id!!
            }
            handler.getCase(currentCaseId!!)
            conditionHintsForCase = handler.conditionHintsForCase(currentCaseId!!)
        }
    }
    LaunchedEffect(verifiedText, indexOfSelectedDiff) {
        if (verifiedText != null || indexOfSelectedDiff != -1) {
            delay(DEBOUNCE_WAIT_PERIOD_MILLIS)
            handler.saveCase(currentCase!!)
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
                override var updateCase = { id: Long ->
                    currentCaseId = id
                }

                override fun onStartRule(selectedDiff: Diff) {
                    handler.startRuleSession(SessionStartRequest(currentCaseId!!, selectedDiff))
                }

                override var onInterpretationEdited: (text: String) -> Unit = {
                    verifiedText = it
                    val updatedCase = currentCase.copy(
                        viewableInterpretation = currentCase.viewableInterpretation
                            .copy(verifiedText = verifiedText)
                    )
                    handler.saveCase(updatedCase)
                }
                override var isCornerstone: Boolean = false
                override var caseEdited: () -> Unit = {}
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

            Spacer(modifier = Modifier.width(10.dp))
            RuleMaker(conditionHintsForCase, object : RuleMakerHandler, Handler by handler {
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