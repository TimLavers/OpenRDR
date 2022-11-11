import io.rippledown.model.CasesInfo
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span
import react.useEffectOnce
import react.useState
import web.timers.setInterval
import kotlin.time.Duration.Companion.seconds

const val NUMBER_OF_CASES_WAITING_ID = "number_of_cases_waiting_value"
val POLL_PERIOD = 0.5.seconds

external interface CaseQueueHandler : Handler

val CaseQueue = FC<CaseQueueHandler> { handler ->
    var waitingCasesInfo by useState(CasesInfo(emptyList(), ""))

    useEffectOnce {
        setInterval(delay = POLL_PERIOD) {
            handler.scope.launch {
                val wci = handler.api.waitingCasesInfo()
                waitingCasesInfo = wci
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

    if (waitingCasesInfo.count > 0) {
        CaseList {
            scope = handler.scope
            api = handler.api
            caseIds = waitingCasesInfo.caseIds
        }
    }
}