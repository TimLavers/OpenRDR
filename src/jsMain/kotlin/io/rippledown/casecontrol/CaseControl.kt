package io.rippledown.casecontrol

import Handler
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.launch
import mui.material.Grid
import react.FC
import react.useState
import xs

/**
 * Provides the facility for the user to select a case from the list and then inspect it.
 */

external interface CaseControlHandler : Handler {
    var caseIds: List<CaseId>
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
                xs = 4
                key = currentCase!!.id.toString()

                CaseInspection {
                    scope = handler.scope
                    api = handler.api
                    case = currentCase!!
                    updateCase = { id ->
                        updateCurrentCase(id)
                    }
                    ruleSessionInProgress = { inProgress ->
                        showSelector = !inProgress
                    }
                }
            }
        }
    }
}