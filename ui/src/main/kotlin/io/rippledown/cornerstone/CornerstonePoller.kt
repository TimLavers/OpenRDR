package io.rippledown.cornerstone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import kotlin.time.Duration.Companion.seconds

interface CornerstonePollerHandler {
    var onUpdate: (updated: CornerstoneStatus) -> Unit
    var updateCornerstoneStatus: () -> CornerstoneStatus
    var isClosing: () -> Boolean
}

val POLL_PERIOD = 2.seconds

@Composable
fun CornerstonePoller(handler: CornerstonePollerHandler, dispatcher: CoroutineDispatcher = MainUIDispatcher) {
    LaunchedEffect(Unit) {
        while (true) {
            if (dispatcher == MainUIDispatcher) {
                // Avoid blocking the UI.
                withContext(dispatcher) {
                    cornerstoneStatus(handler)
                }
            } else {
                // On a test thread so we can launch directly.
                cornerstoneStatus(handler)
            }
            if (handler.isClosing()) break
            delay(POLL_PERIOD)
        }
    }
}

fun cornerstoneStatus(handler: CornerstonePollerHandler) {
    val updatedCasesInfo = handler.updateCornerstoneStatus()
    handler.onUpdate(updatedCasesInfo)
}