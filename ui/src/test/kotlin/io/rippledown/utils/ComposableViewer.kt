package io.rippledown.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rippledown.constants.main.TITLE
import io.rippledown.main.DEFAULT_WINDOW_SIZE
import openrdr.ui.generated.resources.Res
import openrdr.ui.generated.resources.water_wave_icon
import org.jetbrains.compose.resources.painterResource

fun applicationFor(block: @Composable () -> Unit) {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource(Res.drawable.water_wave_icon),
            title = TITLE,
            state = rememberWindowState(size = DEFAULT_WINDOW_SIZE)
        ) {
            block()
        }
    }
}