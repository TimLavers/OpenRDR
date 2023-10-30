package io.rippledown.casecontrol

import Handler
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.launch
import mui.material.Typography
import react.FC
import react.memo
import react.useEffectOnce
import react.useState
import web.timers.setInterval
import kotlin.time.Duration.Companion.seconds

val POLL_PERIOD = 0.5.seconds

external interface CasePollerHandler : Handler {
    var ruleSessionInProgress: (inProgress: Boolean) -> Unit
}

val CasePoller = FC<CasePollerHandler> { handler ->
    var casesInfo by useState(CasesInfo(emptyList(), ""))
    var showCaseCount by useState(true)

    useEffectOnce {
        setInterval(delay = POLL_PERIOD) {
            handler.scope.launch {
                casesInfo = handler.api.waitingCasesInfo()
            }
        }
    }

    if (showCaseCount) {
        Typography {
            +"$CASES ${casesInfo.count}"
            id = NUMBER_OF_CASES_ID
        }
    }

    if (casesInfo.count > 0) {
        CaseControlMemo {
            scope = handler.scope
            api = handler.api
            caseIds = casesInfo.caseIds
            ruleSessionInProgress = { inProgress ->
                showCaseCount = !inProgress
//TODO                handler.ruleSessionInProgress(inProgress)
            }
        }
    }
}
val CaseControlMemo = memo(
    type = CaseControl,
    propsAreEqual = { oldProps, newProps ->
        oldProps.caseIds == newProps.caseIds
    }
)
