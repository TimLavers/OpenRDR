package io.rippledown.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.ktor.client.engine.cio.CIO
import io.rippledown.constants.main.TITLE
import openrdr.ui.generated.resources.Res.drawable
import openrdr.ui.generated.resources.water_wave_icon
import org.jetbrains.compose.resources.painterResource

val DEFAULT_WINDOW_SIZE = DpSize(1_000.dp, 800.dp)
val EXPANDED_WINDOW_SIZE = DpSize(1_800.dp, 800.dp)

fun main() = application {
    var closing by remember { mutableStateOf(false) }
    var windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }
//    val clientState = ClientState()
    fun resizeWindow(newSize: DpSize) {
        windowSize = newSize
    }
    Window(
        onCloseRequest = {
            exitApplication()
            closing = true
        },
        icon = painterResource(drawable.water_wave_icon),
        title = TITLE,
        state = WindowState(size = windowSize)//allow for resizing
    ) {
        OpenRDRUI(object : Handler {
            override var isClosing = { closing }
            override var api: Api = Api(CIO.create())
            override fun showingCornerstone(isShowingCornerstone: Boolean) {
                if (isShowingCornerstone) resizeWindow(EXPANDED_WINDOW_SIZE) else resizeWindow(DEFAULT_WINDOW_SIZE)
            }
        })
    }
}
