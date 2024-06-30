package io.rippledown.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rippledown.constants.main.TITLE

val DEFAULT_WINDOW_SIZE = DpSize(1_800.dp, 800.dp)

fun main() = application {
    var closing by remember { mutableStateOf(false) }
    Window(
        onCloseRequest = {
            this.exitApplication()
            closing = true
        },
        icon = painterResource("water-wave-icon.png"),
        title = TITLE,
        state = rememberWindowState(size = DEFAULT_WINDOW_SIZE)
    ) {
        OpenRDRUI(object : Handler {
            override var isClosing = { closing }
            override var api: Api = Api()
            override var setInfoMessage: (String) -> Unit = {}
        })
    }
}
