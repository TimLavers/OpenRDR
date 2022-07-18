import api.getWaitingCasesInfo
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span

private val scope = MainScope()

val CaseQueue = FC<Props> {
    var waitingCasesInfo by useState(CasesInfo(0, ""))

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
    button {
        +"Refresh"
        onClick = {
            scope.launch {
                waitingCasesInfo = getWaitingCasesInfo()
            }
        }
        id = "refresh_waiting_cases_info_button"
    }
}