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
import kotlin.js.Date

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
            console.log("${Date().toISOString()} Getting waiting cases from useEffectOnce")
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
                        console.log("${Date().toISOString()} Getting waiting cases from refresh button")
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
                        console.log("${Date().toISOString()} Getting waiting cases from REVIEW button")
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
                console.log("${Date().toISOString()} Case queue: about to save interpretation")
                //maybe retrieve the next case or null, rather than case ids
                api.saveInterpretation(interpretation)
                console.log("${Date().toISOString()} saved interpretation")
                val wci = api.waitingCasesInfo()
                console.log("${Date().toISOString()} retrieved waiting cases info $wci")
                caseIds = wci.caseIds
//                caseIds = waitingCasesInfo.caseIds
                console.log("${Date().toISOString()}  updated case ids $caseIds")
                showCaseList = true
                if (caseIds.isNotEmpty()) {
                    val toSelect = caseIds[0]
                    handler.scope.launch {

                        selectedCase = api.getCase(toSelect.id)
                    }
                } else {
                    selectedCase = null
                }
            }
            currentCase = selectedCase
        }
    }
}