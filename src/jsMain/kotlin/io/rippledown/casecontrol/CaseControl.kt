package io.rippledown.casecontrol

import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.main.Handler
import io.rippledown.main.xs
import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.launch
import mui.material.Grid
import mui.material.Typography
import react.FC
import react.memo
import react.useState

/**
 * Provides the facility for the user to select a case from the list and then inspect it.
 */

external interface CaseControlHandler : Handler {
    var caseIds: List<CaseId>
    var ruleSessionInProgress: (inProgress: Boolean) -> Unit
}

val CaseControl = FC<CaseControlHandler> { handler ->
    var currentCase: ViewableCase? by useState(null)
    var showSelector: Boolean by useState(true)

    fun updateCurrentCase(id: Long) {
        handler.scope.launch {
            currentCase = handler.api.getCase(id)
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
        if (showSelector) {

            Grid {
                item = true
                id = CASELIST_ID
                xs = 2

                Typography {
                    +"$CASES ${handler.caseIds.size}"
                    id = NUMBER_OF_CASES_ID
                }

                CaseSelector {
                    caseIds = handler.caseIds
                    selectedCaseName = currentCase?.name
                    selectCase = { id ->
                        updateCurrentCase(id)
                    }
                }
            }
        }

        if (currentCase != null) {
            Grid {
                item = true
                xs = 10
                key = caseInspectionKey(currentCase!!)
                CaseInspectionMemo {
                    scope = handler.scope
                    api = handler.api
                    case = currentCase!!
                    updateCase = { id ->
                        updateCurrentCase(id)
                    }
                    ruleSessionInProgress = { inProgress ->
                        showSelector = !inProgress
                        handler.ruleSessionInProgress(inProgress)
                    }
                }
            }
        }
    }
}

//Include the attribute list in the key so that if they are reordered the case will be re-rendered
fun caseInspectionKey(case: ViewableCase) = "${case.id} ${case.attributes().hashCode()}"

private fun sameCaseId(viewableCase1: ViewableCase, viewableCase2: ViewableCase) =
    viewableCase1.id!! == viewableCase2.id

val CaseInspectionMemo = memo(
    type = CaseInspection,
    propsAreEqual = { oldProps, newProps ->
        sameCaseId(oldProps.case, newProps.case)
    }
)