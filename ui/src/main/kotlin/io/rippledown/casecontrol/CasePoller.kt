package io.rippledown.casecontrol

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

interface CasePollerHandler {
    var onUpdate: (updated: CasesInfo) -> Unit
    var updateCasesInfo: () -> CasesInfo
    var isClosing: () -> Boolean
}

val POLL_PERIOD = 2.seconds

@Composable
fun CasePoller(handler: CasePollerHandler) {
    LaunchedEffect(Unit) {
        queryForCases(handler = handler)
        while (!handler.isClosing()) {
            delay(POLL_PERIOD)
            queryForCases(handler = handler)
        }
    }
}

fun queryForCases(handler: CasePollerHandler) {
    val updatedCasesInfo = handler.updateCasesInfo()
    handler.onUpdate(updatedCasesInfo)
}

