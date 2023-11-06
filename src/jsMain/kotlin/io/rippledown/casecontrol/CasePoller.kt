package io.rippledown.casecontrol

import io.rippledown.kb.KBInfoPane
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.launch
import main.Handler
import react.FC
import react.memo
import react.useEffectOnce
import react.useState
import web.timers.setInterval
import kotlin.time.Duration.Companion.seconds

val POLL_PERIOD = 0.5.seconds

external interface CasePollerHandler : Handler

val CasePoller = FC<CasePollerHandler> { handler ->
    var casesInfo by useState(CasesInfo(emptyList(), ""))
    var showKB by useState(true)

    useEffectOnce {
        setInterval(delay = POLL_PERIOD) {
            handler.scope.launch {
                casesInfo = handler.api.waitingCasesInfo()
            }
        }
    }

    KBInfoPane {
        scope = handler.scope
        api = handler.api
        showKBInfo = showKB
    }

    if (casesInfo.count > 0) {
        CaseControlMemo {
            scope = handler.scope
            api = handler.api
            caseIds = casesInfo.caseIds
            ruleSessionInProgress = { inProgress ->
                showKB = !inProgress
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
