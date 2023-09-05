package io.rippledown.casecontrol

import Handler
import debug
import io.rippledown.caseview.CaseViewMemo
import io.rippledown.cornerstoneview.CornerstoneView
import io.rippledown.interpretation.ConditionSelector
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.interpretation.diffViewerKey
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlinx.coroutines.launch
import mui.material.Box
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
    var updatedInterpretation: Interpretation by useState(handler.case.interpretation)

    val id = handler.case.id!!
    debug("CaseInspection: case ${handler.case.id} interp $updatedInterpretation}}")

    Grid {
        container = true
        Grid {
            item = true
            xs = 4

            Box {
                key = interpretationTabsKey(updatedInterpretation)
                CaseViewMemo {
                    scope = handler.scope
                    api = handler.api
                    case = handler.case
                    onCaseEdited = {
                        handler.updateCase(handler.case.id!!)
                    }
                }

                InterpretationTabs {
                    scope = handler.scope
                    api = handler.api
                    interpretation = updatedInterpretation
                    onStartRule = { selectedDiff ->
                        handler.scope.launch {
                            val sessionStartRequest = SessionStartRequest(id, selectedDiff)
                            ccStatus = handler.api.startRuleSession(sessionStartRequest)
                            conditionHints = handler.api.conditionHints(id)
                            handler.ruleSessionInProgress(true)
                        }
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
                        scope.launch {
                            ccStatus = api.selectCornerstone(index)
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
                        handler.ruleSessionInProgress(false)
                        ccStatus = null
                    }
                    conditionSelected = { conditions ->
                        handler.scope.launch {
                            val updateCCRequest = UpdateCornerstoneRequest(
                                ccStatus!!,
                                ConditionList(conditions)
                            )
                            debug("CaseInspection: updateCornerstoneRequest $updateCCRequest")
                            ccStatus = handler.api.updateCornerstoneStatus(updateCCRequest)
                        }
                    }
                    onDone = { conditionList ->
                        scope.launch {
                            val ruleRequest = RuleRequest(
                                caseId = handler.case.rdrCase.caseId.id!!,
                                conditions = ConditionList(conditions = conditionList)
                            )
                            updatedInterpretation = api.buildRule(ruleRequest)
                            handler.ruleSessionInProgress(false)
                            ccStatus = null
                        }
                    }
                }
            }
        }
    }
}

/**
 * Re-render when the Diff List changes
 */
fun interpretationTabsKey(interp: Interpretation) = with(interp) {
    "${diffViewerKey(diffList)}-${latestText()}"
}
