import api.getCase
import api.getWaitingCasesInfo
import csstype.*
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*
import react.css.css
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span

private val scope = MainScope()

val CaseQueue = FC<Props> {
    var waitingCasesInfo by useState(CasesInfo(emptyList(), ""))
    var showCaseList: Boolean by useState(false)
    var selectedCase: RDRCase? by useState(null)

    useEffectOnce {
        scope.launch {
            waitingCasesInfo = getWaitingCasesInfo()
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
            id = "number_of_cases_waiting_value"
        }
    }
    span {
        div {
            css {
                padding = Length("12px")
            }
            button {
                +"Refresh"
                onClick = {
                    scope.launch {
                        waitingCasesInfo = getWaitingCasesInfo()
                    }
                }
                id = "refresh_waiting_cases_info_button"
            }
            button {
                +"Review"
                onClick = {
                    scope.launch {
                        waitingCasesInfo = getWaitingCasesInfo()
                        showCaseList = true
                    }
                }
                disabled = waitingCasesInfo.count == 0
                id = "review_cases_button"
            }
        }
    }
    if (showCaseList) {
        CaseList {
            caseIds = waitingCasesInfo.caseIds
            onCaseSelected = {
                console.log("Selected: ", it)
                scope.launch {
                    selectedCase = getCase(it)
                }
            }
            currentCase = selectedCase
        }
    }
}