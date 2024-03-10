package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
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
@Preview
fun CasePoller(handler: CasePollerHandler) {

    LaunchedEffect(Unit) {
        while (!handler.isClosing()) {
            val updatedCasesInfo = handler.updateCasesInfo()
            handler.onUpdate(updatedCasesInfo)
            delay(POLL_PERIOD)
        }
    }
}