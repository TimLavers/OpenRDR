package io.rippledown.casecontrol

import Handler
import debug
import io.rippledown.caseview.CaseView
import io.rippledown.cornerstoneview.CornerstoneView
import io.rippledown.interpretation.ConditionSelector
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlinx.coroutines.launch
import mui.material.Grid
import react.FC
import react.useState
import xs

external interface CaseInspectionHandler : Handler {
    var case: ViewableCase
    var updateCase: (Long) -> Unit
    var ruleSessionInProgress: (Boolean) -> Unit
}

val CaseInspection = FC<CaseInspectionHandler> { handler ->
    var ccStatus: CornerstoneStatus? by useState(null)
    var conditionHints: ConditionList? by useState(null)

    val currentCase = handler.case
    val id: Long = currentCase.id!!

    Grid {
        container = true
        Grid {
            item = true
            xs = 4

            CaseView {
                scope = handler.scope
                api = handler.api
                case = currentCase
                onCaseEdited = {
                    handler.updateCase(id)
                }
                onStartRule = { selectedDiff ->
                    handler.scope.launch {
                        val hints = handler.api.conditionHints(id)
                        val sessionStartRequest = SessionStartRequest(id, selectedDiff)
                        val cs = handler.api.startRuleSession(sessionStartRequest)
                        conditionHints = hints
                        ccStatus = cs
                        handler.ruleSessionInProgress(true)
                    }
                }
            }
        }

        Grid {
            item = true
            xs = 4
            if (ccStatus != null) {
                CornerstoneView {
                    scope = handler.scope
                    api = handler.api
                    cornerstoneStatus = ccStatus!!
                    selectCornerstone = { index ->
                        handler.scope.launch {
                            ccStatus = handler.api.selectCornerstone(index)
                        }
                    }
                }
            }
        }

        Grid {
            item = true
            xs = 2
            if (ccStatus != null && conditionHints != null) {
                ConditionSelector {
                    scope = handler.scope
                    api = handler.api
                    conditions = conditionHints!!.conditions
                    onCancel = {
                        ccStatus = null
                    }
                    conditionSelected = { conditions ->
                        handler.scope.launch {
                            val updateCCRequest = UpdateCornerstoneRequest(
                                ccStatus!!,
                                ConditionList(conditions)
                            )
                            ccStatus = handler.api.updateCornerstoneStatus(updateCCRequest)
                        }
                    }
                    onDone = { conditionList ->
                        debug("CaseInspection: onDone1: caseid=${currentCase.rdrCase.caseId.id}")
                        val ruleRequest = RuleRequest(
                            caseId = currentCase.rdrCase.caseId.id!!,
                            conditionList = ConditionList(conditions = conditionList)
                        )
                        debug("0 rulerquest=$ruleRequest")
                        handler.scope.launch {
                            debug("1 rulerquest=$ruleRequest")
                            val retInterp = handler.api.buildRule(ruleRequest)
                            debug("2")
//                            handler.ruleSessionInProgress(false)
//                            debug("3")
//                            handler.updateCase(currentCase.rdrCase.caseId.id)
//                            debug("CaseInspection: onDone2: caseid=${currentCase.rdrCase.caseId.id}")
//                            ccStatus = null
                        }
                    }
                }
            }
        }
    }
}
