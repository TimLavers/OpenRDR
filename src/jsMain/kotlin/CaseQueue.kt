import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span
import react.useEffectOnce
import react.useState

const val NUMBER_OF_CASES_WAITING_ID = "number_of_cases_waiting_value"
const val REFRESH_BUTTON_ID = "refresh_waiting_cases_info_button"
const val REVIEW_CASES_BUTTON_ID = "review_cases_button"

external interface CaseQueueHandler : Props {
    var scope: CoroutineScope
    var api: Api
}

val CaseQueue = FC<CaseQueueHandler> { handler ->
    var waitingCasesInfo by useState(CasesInfo(emptyList(), ""))
    var showCaseList: Boolean by useState(false)
    var selectedCase: RDRCase? by useState(null)
    val api = handler.api

    useEffectOnce {
        handler.scope.launch {
            waitingCasesInfo = api.waitingCasesInfo()
        }
    }

    h2 {
        +"Review queue"
        id = "number_of_cases_waiting_heading"
    }
    div {
        +"Cases waiting: "
        span {
            +"${waitingCasesInfo.count}"
            id = NUMBER_OF_CASES_WAITING_ID
        }
    }
    span {
        div {
            button {
                +"Refresh"
                id = REFRESH_BUTTON_ID
                css {
                    padding = px4
                }
                onClick = {
                    handler.scope.launch {
                        waitingCasesInfo = api.waitingCasesInfo()
                    }
                }
            }
            button {
                +"Review"
                id = REVIEW_CASES_BUTTON_ID
                css {
                    padding = px4
                }
                onClick = {
                    handler.scope.launch {
                        waitingCasesInfo = api.waitingCasesInfo()
                        showCaseList = true
                    }
                }
                disabled = waitingCasesInfo.count == 0
            }
        }
    }
    if (showCaseList) {
        CaseList {
            scope = handler.scope
            caseIds = waitingCasesInfo.caseIds
            onCaseSelected = {
                handler.scope.launch {
                    selectedCase = handler.api.getCase(it)
                }
            }
            onCaseProcessed = { interpretation ->
                //maybe retrieve the next case or null, rather than case ids
                handler.scope.launch {
                    api.saveInterpretation(interpretation)
                    waitingCasesInfo = api.waitingCasesInfo()
                    caseIds = waitingCasesInfo.caseIds
                    showCaseList = true
                    if (waitingCasesInfo.count > 0) {
                        val toSelect = waitingCasesInfo.caseIds[0]
                        handler.scope.launch {
                            selectedCase = api.getCase(toSelect.id)
                        }
                    } else {
                        selectedCase = null
                    }
                }
            }
            currentCase = selectedCase
        }
    }
}