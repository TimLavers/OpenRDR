package io.rippledown.caselist

import Handler
import debug
import io.rippledown.caseview.CaseView
import io.rippledown.cornerstoneview.CornerstoneView
import io.rippledown.interpretation.ConditionSelector
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.launch
import mui.material.Grid
import react.FC
import react.memo
import react.useState
import xs

const val CASELIST_ID = "case_list_container"
const val CASE_ID_PREFIX = "case_list_item_"

external interface CaseListHandler : Handler {
    var caseIds: List<CaseId>
}

val CaseList = FC<CaseListHandler> { handler ->
    var currentCase: ViewableCase? by useState(null)
    var ccStatus: CornerstoneStatus? by useState(null)
    var newInterpretation: Interpretation? by useState(null)
    var conditionHints: ConditionList? by useState(null)

    fun updateCurrentCase(id: String) {
        handler.scope.launch {
            debug("about to update current case with id $id")
            val current = handler.api.getCase(id)
            debug("updated current case with id $id. Interp was ${current.interpretation} latest text was ${current.interpretation.latestText()}")
            currentCase = current
        }
    }

    fun selectFirstCase() {
        val names = handler.caseIds.map { it.name }
        val currentCaseNullOrNotAvailable = currentCase == null || !names.contains(currentCase?.name)
        if (currentCaseNullOrNotAvailable && names.isNotEmpty()) {
            val firstCaseId = handler.caseIds[0]
            debug("about to get first case with id ${firstCaseId.id}")
            updateCurrentCase(firstCaseId.id)
            debug("got first case with case id ${firstCaseId.id}")
        }
    }
    selectFirstCase()

    Grid {
        container = true
        if (ccStatus == null) {
            Grid {
                item = true
                id = CASELIST_ID
                xs = 2
                CaseSelector {
                    caseIds = handler.caseIds
                    selectedCaseName = currentCase?.name
                    selectCase = { id ->
                        debug("caselist selected case with id $id")
                        updateCurrentCase(id)
                    }
                }
            }
        }
        Grid {
            item = true
            xs = 4
            if (currentCase != null) {
                val id = currentCase!!.id
                CaseView {
                    scope = handler.scope
                    api = handler.api
                    debug("caseview with id $id and interp ${currentCase!!.interpretation.latestText()}")
                    case = currentCase!!
                    onCaseEdited = {
                        updateCurrentCase(id)
                    }
                    onStartRule = { newInterp ->
                        newInterpretation = newInterp
                        handler.scope.launch {
                            conditionHints = handler.api.conditionHints(id)
                            val sessionStartRequest = SessionStartRequest(id, newInterp.diffList.selectedChange())
                            debug("$sessionStartRequest")
                            val cc = handler.api.startRuleSession(sessionStartRequest)
                            debug("cc status: $cc")
                            ccStatus = cc
                        }
                    }
                }
            }
        }
        Grid {
            item = true
            xs = 4
            if (ccStatus != null ) {
                debug("showing cornerstone view")
                CornerstoneView {
                    scope = handler.scope
                    api = handler.api
                    cornerstoneStatus = ccStatus!!
                }
            }
        }

        Grid {
            item = true
            xs = 2
            if (newInterpretation != null && conditionHints != null) {
                ConditionSelector {
                    scope = handler.scope
                    api = handler.api
                    conditions = conditionHints!!.conditions
                    onCancel = {
                        newInterpretation = null
                    }
                    onDone = { conditionList ->
                        handler.scope.launch {
                            val ruleRequest = RuleRequest(
                                caseId = currentCase!!.id,
                                diffList = newInterpretation!!.diffList,
                                conditionList = ConditionList(conditions = conditionList)
                            )
                            handler.api.buildRule(ruleRequest)
                            newInterpretation = null
                            ccStatus = null
                            updateCurrentCase(currentCase!!.id)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Memoize the CaseList component so that it is not re-rendered when the caseIds don't change.
 */
val CaseListMemo = memo(
    type = CaseList,
    propsAreEqual = { prevProps, nextProps ->
        prevProps.caseIds == nextProps.caseIds
    }
)
