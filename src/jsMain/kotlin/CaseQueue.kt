import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.MainScope
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

private val scope = MainScope()

external interface CaseQueueHandler : Props {
    var getWaitingCasesInfo: suspend () -> CasesInfo
}

val CaseQueue = FC<CaseQueueHandler> { props ->
    var waitingCasesInfo by useState(CasesInfo(emptyList(), ""))
    var showCaseList: Boolean by useState(false)
    var selectedCase: RDRCase? by useState(null)

    useEffectOnce {
        scope.launch {
            waitingCasesInfo = props.getWaitingCasesInfo()
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
                    scope.launch {
                        waitingCasesInfo = props.getWaitingCasesInfo()
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
                    scope.launch {
                        waitingCasesInfo = props.getWaitingCasesInfo()
                        showCaseList = true
                    }
                }
                disabled = waitingCasesInfo.count == 0
            }
        }
    }
    if (showCaseList) {
        CaseList {
            caseIds = waitingCasesInfo.caseIds
            onCaseSelected = {
                scope.launch {
                    selectedCase = ApiClient().getCase(it)
                }
            }
            onCaseProcessed = { interpretation ->
                //maybe retrieve the next case or null, rather than case ids
                scope.launch {
                    ApiClient().saveInterpretation(interpretation)
                    waitingCasesInfo = props.getWaitingCasesInfo()
                    caseIds = waitingCasesInfo.caseIds
                    showCaseList = true
                    if (waitingCasesInfo.count > 0) {
                        val toSelect = waitingCasesInfo.caseIds[0]
                        scope.launch {
                            selectedCase = ApiClient().getCase(toSelect.id)
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