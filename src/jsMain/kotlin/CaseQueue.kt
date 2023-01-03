import io.rippledown.model.CasesInfo
import kotlinx.coroutines.launch
import mui.material.Typography
import react.FC
import react.useEffectOnce
import react.useState
import web.timers.setInterval
import kotlin.time.Duration.Companion.seconds

const val NUMBER_OF_CASES_WAITING_ID = "number_of_cases_waiting_value"
val POLL_PERIOD = 5.seconds

external interface CaseQueueHandler : Handler

val CaseQueue = FC<CaseQueueHandler> { handler ->
    var waitingCasesInfo by useState(CasesInfo(emptyList(), ""))

    useEffectOnce {
        setInterval(delay = POLL_PERIOD) {
            handler.scope.launch {
                waitingCasesInfo = handler.api.waitingCasesInfo()
            }
        }
    }

    Typography {
        +"Cases waiting: ${waitingCasesInfo.count}"
        id = NUMBER_OF_CASES_WAITING_ID
    }

    if (waitingCasesInfo.count > 0) {
        CaseList {
            scope = handler.scope
            api = handler.api
            caseIds = waitingCasesInfo.caseIds
        }
    }
}