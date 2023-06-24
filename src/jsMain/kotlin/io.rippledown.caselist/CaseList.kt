package io.rippledown.caselist

import io.rippledown.caseview.CaseView
import Handler
import io.rippledown.interpretation.ConditionSelector
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.RuleRequest
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
    var newInterpretation: Interpretation? by useState(null)
    var conditionHints: ConditionList? by useState(null)

    fun updateCurrentCase(id: Long) {
        handler.scope.launch {
            val returned = handler.api.getCase(id)
            currentCase = returned
        }
    }

    fun selectFirstCase() {
        val names = handler.caseIds.map { it.name }
        val currentCaseNullOrNotAvailable = currentCase == null || !names.contains(currentCase?.name)
        if (currentCaseNullOrNotAvailable && names.isNotEmpty()) {
            val firstCaseId = handler.caseIds[0]
            updateCurrentCase(firstCaseId.id!!)
        }
    }
    selectFirstCase()

    Grid {
        container = true
        Grid {
            item = true
            id = CASELIST_ID
            xs = 2
            CaseSelector{
                caseIds = handler.caseIds
                selectedCaseName = currentCase?.name
                selectCase = { id ->
                    updateCurrentCase(id)
                }
            }
        }
        Grid {
            item = true
            xs = 6
            if (currentCase != null) {
                CaseView {
                    scope = handler.scope
                    api = handler.api
                    case = currentCase!!
                    onCaseEdited = {
                        updateCurrentCase(currentCase!!.rdrCase.caseId.id!!)
                    }
                    onStartRule = { newInterp ->
                        newInterpretation = newInterp
                        handler.scope.launch {
                            conditionHints = handler.api.conditionHints(currentCase!!.id!!)
                        }
                    }
                }
            }
        }

        Grid {
            item = true
            xs = 4
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
                                caseId = currentCase!!.rdrCase.caseId.id!!,
                                diffList = newInterpretation!!.diffList,
                                conditionList = ConditionList(conditions = conditionList)
                            )
                            handler.api.buildRule(ruleRequest)
                            newInterpretation = null
                            updateCurrentCase(currentCase!!.rdrCase.caseId.id!!)
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
