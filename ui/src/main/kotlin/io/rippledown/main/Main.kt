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
import io.rippledown.constants.main.TITLE

val DEFAULT_WINDOW_SIZE = DpSize(1_000.dp, 800.dp)
val EXPANDED_WINDOW_SIZE = DpSize(1_400.dp, 800.dp)

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
        OpenRDRUI(object : Handler {
            override var isClosing = { closing }
            override var api: Api = Api()
            override fun setWindowSize(isShowingCornerstone: Boolean) {
                val size = if (isShowingCornerstone) EXPANDED_WINDOW_SIZE else DEFAULT_WINDOW_SIZE
                resizeWindow(size)
            }
        })
    }
}
