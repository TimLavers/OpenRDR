import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.launch
import mui.material.Typography
import react.FC
import react.useEffectOnce
import react.useState
import web.timers.setInterval
import kotlin.time.Duration.Companion.seconds

val POLL_PERIOD = 0.5.seconds

external interface CasePollerHandler : Handler

val CasePoller = FC<CasePollerHandler> { handler ->
    var casesInfo by useState(CasesInfo(emptyList(), ""))

    useEffectOnce {
        setInterval(delay = POLL_PERIOD) {
            handler.scope.launch {
                casesInfo = handler.api.waitingCasesInfo()
            }
        }
    }

    Typography {
        +"$CASES ${casesInfo.count}"
        id = NUMBER_OF_CASES_ID
    }

    if (casesInfo.count > 0) {
        CaseListMemo {
            scope = handler.scope
            api = handler.api
            caseIds = casesInfo.caseIds
        }
    }
}