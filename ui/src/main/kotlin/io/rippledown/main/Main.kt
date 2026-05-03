package io.rippledown.main

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE

val DEFAULT_WINDOW_SIZE = DpSize(1_000.dp, 800.dp)
val EXPANDED_WINDOW_SIZE = DpSize(1_400.dp, 800.dp)

/**
 * Build the [Handler] (and its single [Api]) once, not on every recomposition.
 *
 * Without [remember] each recomposition of the enclosing window would
 * construct a fresh [Api] whose `currentKB` is null, silently dropping the
 * selected KB and routing the next request to the server's default KB
 * (Thyroids). Symptoms downstream included the chat panel hitting an
 * uninitialised `ChatSessionManager`. The unit test in
 * `MainHandlerStabilityTest` pins this behaviour.
 */
@Composable
internal fun rememberMainHandler(
    isClosing: () -> Boolean,
    resizeWindow: (DpSize) -> Unit,
    apiFactory: () -> Api = { Api() }
): Handler = remember {
    object : Handler {
        override var isClosing = isClosing
        override var api: Api = apiFactory()
        override fun setWindowSize(isShowingCornerstone: Boolean) {
            val size = if (isShowingCornerstone) EXPANDED_WINDOW_SIZE else DEFAULT_WINDOW_SIZE
            resizeWindow(size)
        }
    }
}

fun main() = application {
    var closing by remember { mutableStateOf(false) }
    var windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }

    fun resizeWindow(newSize: DpSize) {
        windowSize = newSize
    }
    Window(
        onCloseRequest = {
            exitApplication()
            closing = true
        },
        title = TITLE,
        state = WindowState(size = windowSize)//allow for resizing
    ) {
        applyAppIcon(window)
        OpenRDRUI(rememberMainHandler(isClosing = { closing }, resizeWindow = ::resizeWindow))
    }
}
