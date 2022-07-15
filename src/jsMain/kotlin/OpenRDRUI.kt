import io.rippledown.model.CasesInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2

private val scope = MainScope()

val OpenRDRUI = FC<Props> {
    var waitingCasesInfo by useState(CasesInfo(0, ""))

    useEffectOnce {
        scope.launch {
            waitingCasesInfo = getWaitingCasesInfo()
        }
    }

    h1 {
        +"Open RippleDown"
        id = "main_heading"
    }
    h2 {
        +"Number of cases waiting"
        id = "number_of_cases_waiting_heading"
    }
    div {
        +waitingCasesInfo.resourcePath
        id = "number_of_cases_waiting_path"
    }
    div {
        +"${waitingCasesInfo.count}"
        id = "number_of_cases_waiting_value"
    }
}