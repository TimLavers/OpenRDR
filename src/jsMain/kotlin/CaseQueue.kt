import emotion.react.css
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span
import react.useEffectOnce
import react.useState
import web.timers.setInterval
import kotlin.time.Duration.Companion.seconds

const val NUMBER_OF_CASES_WAITING_ID = "number_of_cases_waiting_value"
const val REFRESH_BUTTON_ID = "refresh_waiting_cases_info_button"
const val REVIEW_CASES_BUTTON_ID = "review_cases_button"

external interface CaseQueueHandler : Handler {
}

val CaseQueue = FC<CaseQueueHandler> { handler ->
    var waitingCasesInfo by useState(CasesInfo(emptyList(), ""))
    var showCaseList: Boolean by useState(false)
    var selectedCase: RDRCase? by useState(null)

    useEffectOnce {
        console.log("\n\ncall set interval")
        setInterval(2.seconds) {
            console.log("\n\nlaunching waitingCasesInfo:")
            handler.scope.launch {
                console.log("\n\ncall waitingCasesInfo:")
                waitingCasesInfo = handler.api.waitingCasesInfo()
            }
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
                        waitingCasesInfo = handler.api.waitingCasesInfo()
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
                        waitingCasesInfo = handler.api.waitingCasesInfo()
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
            api = handler.api
            caseIds = waitingCasesInfo.caseIds
            currentCase = selectedCase
            onCaseSelected = {
                scope.launch {
                    selectedCase = api.getCase(it)
                }
            }
            onInterpretationSubmitted = {
                scope.launch {
                    val waiting = api.waitingCasesInfo()
                    caseIds = waiting.caseIds
                    if (caseIds.isNotEmpty()) {
                        currentCase = api.getCase(caseIds[0].id)
                        waitingCasesInfo = waiting
                        selectedCase = currentCase
                        showCaseList = true
                    } else {
                        currentCase = null
                        showCaseList = false
                        waitingCasesInfo = CasesInfo()
                    }
                }
            }
        }
    }
}