package io.rippledown.casecontrol

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import kotlin.time.Duration.Companion.seconds

interface CasePollerHandler {
    var onUpdate: (updated: CasesInfo) -> Unit
    var updateCasesInfo: () -> CasesInfo
    var isClosing: () -> Boolean
}

val POLL_PERIOD = 2.seconds

@Composable
fun CasePoller(handler: CasePollerHandler, dispatcher: CoroutineDispatcher = MainUIDispatcher) {
    LaunchedEffect(Unit) {
        while (true) {
            if (dispatcher == MainUIDispatcher) {
                // If we are on the main thread, we need to use a CoroutineScope to launch the query
                // to avoid blocking the UI.
                withContext(dispatcher) {
                    queryForCases(handler)
                }
            } else {
                // If we are not on a test thread, we can launch directly.
                queryForCases(handler)
            }
            //Allow one poll to complete before checking if we should close. Some tests rely on
            //at least one poll, but will block on the EDT if the polling is not stopped due to the
            //endless recompositions and the fact that the delay has no effect on the test context (Dispatchers.Unconfined).
            if (handler.isClosing()) break
            delay(POLL_PERIOD)
        }
    }
}

fun queryForCases(handler: CasePollerHandler) {
    val updatedCasesInfo = handler.updateCasesInfo()
    handler.onUpdate(updatedCasesInfo)
}