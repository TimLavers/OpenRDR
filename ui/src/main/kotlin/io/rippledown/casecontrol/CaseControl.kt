package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
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
import io.rippledown.rule.RuleMaker
import io.rippledown.rule.RuleMakerHandler
import kotlinx.coroutines.delay

interface CaseControlHandler : Handler, CaseInspectionHandler {
    var setRuleInProgress: (_: Boolean) -> Unit
    var getCase: (caseId: Long) -> ViewableCase?
    suspend fun saveCase(case: ViewableCase): ViewableCase
    suspend fun conditionHintsForCase(caseId: Long): List<Condition>
    fun startRuleSession(sessionStartRequest: SessionStartRequest): CornerstoneStatus
    fun buildRule(ruleRequest: RuleRequest): ViewableCase
}

@Composable
@Preview
fun CaseControl(ruleInProgress: Boolean, casesInfo: CasesInfo, handler: CaseControlHandler) {
    var currentCase: ViewableCase? by remember { mutableStateOf(null) }
    var currentCaseId: Long? by remember { mutableStateOf(null) }
    var verifiedText: String? by remember { mutableStateOf(null) }
    var indexOfSelectedDiff: Int by remember { mutableStateOf(-1) }
    var conditionHintsForCase by remember { mutableStateOf(listOf<Condition>()) }

    LaunchedEffect(casesInfo, currentCaseId) {
        if (casesInfo.caseIds.isNotEmpty()) {
            if (currentCaseId == null || currentCaseId !in casesInfo.caseIds.map { it.id }) {
                //No initial case, or it's now been deleted
                currentCaseId = casesInfo.caseIds[0].id!!
            }
            currentCase = handler.getCase(currentCaseId!!)
            conditionHintsForCase = handler.conditionHintsForCase(currentCaseId!!)
        }
    }
    LaunchedEffect(verifiedText, indexOfSelectedDiff) {
        if (verifiedText != null || indexOfSelectedDiff != -1) {
            delay(DEBOUNCE_WAIT_PERIOD_MILLIS)
            currentCase = handler.saveCase(currentCase!!)
        }
    }

    Row(
        modifier = Modifier
            .padding(10.dp)
    )
    {
        if (!ruleInProgress) {
            Column {
                CaseSelectorHeader(casesInfo.caseIds.size)
                Spacer(modifier = Modifier.height(10.dp))
                CaseSelector(casesInfo.caseIds, object : CaseSelectorHandler, Handler by handler {
                    override var selectCase = { id: Long ->
                        currentCaseId = id
                    }
                })
            }
        }

        if (currentCase != null) {
            CaseInspection(currentCase!!, object : CaseInspectionHandler, Handler by handler {
                override var updateCase = { id: Long ->
                    currentCaseId = id
                }
                override fun onStartRule(selectedDiff: Diff) {
                    handler.startRuleSession(SessionStartRequest(currentCaseId!!, selectedDiff))
                    handler.setRuleInProgress(true)//todo remove
                }
                override var onInterpretationEdited: (text: String) -> Unit = {
                    verifiedText = it
                    currentCase = currentCase!!.copy(
                        viewableInterpretation = currentCase!!.viewableInterpretation
                            .copy(verifiedText = verifiedText)
                    )
                }
                override var isCornerstone: Boolean = false
                override var caseEdited: () -> Unit = {}
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            })
            if (ruleInProgress) {
                Spacer(modifier = Modifier.width(10.dp))
                RuleMaker(conditionHintsForCase, object : RuleMakerHandler, Handler by handler {
                    override var onDone = { conditions: List<Condition> ->
                        handler.setRuleInProgress(false)
                        val ruleRequest = RuleRequest(currentCase!!.id!!, ConditionList(conditions))
                        currentCase = handler.buildRule(ruleRequest)
                    }

                    override var onCancel = {
                        handler.setRuleInProgress(false)
                    }
                })
            } else Spacer(modifier = Modifier.width(310.dp))
        }
    }
}