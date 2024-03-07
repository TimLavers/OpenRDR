package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import io.rippledown.main.Handler
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

interface CasePollerHandler : Handler {
    var updatedCasesInfo: (updated: CasesInfo) -> Unit
}

val POLL_PERIOD = 2.seconds

@Composable
@Preview
fun CasePoller(handler: CasePollerHandler) {
    println("recompose CasePoller ")
    var casesInfo by remember { mutableStateOf(CasesInfo()) }

    LaunchedEffect(Unit) {
        val close = handler.isClosing()
        println("CasePoller close: $close")
        while (!close) {
            delay(POLL_PERIOD)
            val updatedCasesInfo = handler.api.waitingCasesInfo()

            if (updatedCasesInfo != casesInfo) {
                println("########CasePoller: updatedCasesInfo: $updatedCasesInfo")
                casesInfo = updatedCasesInfo
                handler.updatedCasesInfo(updatedCasesInfo)
            }
        }
    }
}