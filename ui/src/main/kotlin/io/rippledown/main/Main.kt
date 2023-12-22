package io.rippledown.main

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        icon = painterResource("water-wave-icon.png"),
        title = TITLE
    ) {
        OpenRDRUI(object : Handler {
            override var api = Api()
        })
    }
}
