package io.rippledown.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rippledown.constants.main.TITLE
import io.rippledown.main.DEFAULT_WINDOW_SIZE

fun applicationFor(block: @Composable () -> Unit) {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource("water-wave-icon.png"),
            title = TITLE,
            state = rememberWindowState(size = DEFAULT_WINDOW_SIZE)
        ) {
            block()
        }
    }
}