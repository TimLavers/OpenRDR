package io.rippledown.casecontrol

import Handler
import io.rippledown.caseview.CaseViewMemo
import io.rippledown.cornerstoneview.CornerstoneView
import io.rippledown.interpretation.ConditionSelector
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.interpretation.diffViewerKey
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.interpretationview.ViewableInterpretation
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
    var updatedInterpretation: ViewableInterpretation by useState(handler.case.viewableInterpretation)

    val caseId = handler.case.id!!

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
                        handler.ruleSessionInProgress(true)
                        handler.scope.launch {
                            val sessionStartRequest = SessionStartRequest(caseId, selectedDiff)
                            ccStatus = handler.api.startRuleSession(sessionStartRequest)
                        }
                        handler.scope.launch {
                            conditionHints = handler.api.conditionHints(caseId)
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
                            ccStatus = handler.api.updateCornerstoneStatus(updateCCRequest)
                        }
                    }
                    onDone = { conditionList ->
                        scope.launch {
                            val ruleRequest = RuleRequest(
                                caseId = handler.case.case.caseId.id!!,
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
fun interpretationTabsKey(interp: ViewableInterpretation) = with(interp) {
    "${diffViewerKey(diffList)}-${latestText()}"
}
