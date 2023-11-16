package io.rippledown.main

import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CasePoller
import kotlinx.coroutines.CoroutineScope
import mui.system.Box
import mui.system.sx
import react.FC
import react.Props
import react.useState
import web.cssom.px

external interface Handler : Props {
    var scope: CoroutineScope
    var api: Api
}

val OpenRDRUI = FC<Handler> { handler ->
    var ruleInProgress by useState(false)

    Box {
        sx {
            paddingTop = 50.px //TODO separate app bar from content
        }

        ApplicationBar {
            scope = handler.scope
            api = handler.api
            isRuleSessionInProgress = ruleInProgress
        }

        CasePoller {
            scope = handler.scope
            api = handler.api
            this.ruleInProgress = { inProgress ->
                ruleInProgress = inProgress
            }
        }
    }
}