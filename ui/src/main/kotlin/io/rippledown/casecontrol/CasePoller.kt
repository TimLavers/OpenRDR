package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import io.rippledown.appbar.AppBarHandler
import io.rippledown.main.Handler
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

interface CasePollerHandler : AppBarHandler {
    var setRuleInProgress: (inProgress: Boolean) -> Unit
    var updatedCasesInfo: (updated: CasesInfo) -> Unit
}

val POLL_PERIOD = 2.seconds

@Composable
@Preview
fun CasePoller(handler: CasePollerHandler) {
    val counter = remember { mutableStateOf(0) }
    println("recompose CasePoller with counter ${counter.value}")
    var casesInfo by remember { mutableStateOf(CasesInfo()) }


    LaunchedEffect(counter.value) {
        delay(POLL_PERIOD)
        val updatedCasesInfo = handler.api.waitingCasesInfo()
        println("CasePoller: CasesInfo: $casesInfo")
        if (updatedCasesInfo != casesInfo) {
            println("CasePoller: updatedCasesInfo: $updatedCasesInfo")
            casesInfo = updatedCasesInfo
            handler.updatedCasesInfo(updatedCasesInfo)
        }
        counter.value++
    }

    if (casesInfo.count > 0) {
        CaseControl(object : CaseControlHandler, Handler by handler {
            override var caseIds = casesInfo.caseIds
            override var setRuleInProgress = handler.setRuleInProgress
        })
    }
}