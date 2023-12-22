package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import io.rippledown.appbar.AppBarHandler
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration.Companion.seconds

interface CasePollerHandler : AppBarHandler {
    var setRuleInProgress: (inProgress: Boolean) -> Unit
}

val POLL_PERIOD = 0.5.seconds

@Composable
@Preview
fun CasePoller(handler: CasePollerHandler) {
    var casesInfo by remember { mutableStateOf(CasesInfo()) }

    LaunchedEffect(Unit) {
        delay(POLL_PERIOD)
        println("CasePoller: poll at ${Date()}")
        casesInfo = handler.api.waitingCasesInfo()
    }

    if (casesInfo.count > 0) {
        println("CasePoller: ${casesInfo.count} cases available")
        CaseControl(object : CaseControlHandler, CasePollerHandler by handler {
            override var caseIds = casesInfo.caseIds
            override var setRuleInProgress = handler.setRuleInProgress
        })
    }
}