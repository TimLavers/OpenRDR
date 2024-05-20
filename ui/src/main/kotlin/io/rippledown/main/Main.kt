package io.rippledown.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE

fun main() = application {
    var closing by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = {
            this.exitApplication()
            closing = true
        },
        icon = painterResource("water-wave-icon.png"),
        title = TITLE
    ) {
        OpenRDRUI(object : Handler {
            override var isClosing = { closing }
            override var api: Api = Api()
        })
    }
}
