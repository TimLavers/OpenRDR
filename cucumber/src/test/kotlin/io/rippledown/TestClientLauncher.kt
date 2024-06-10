@file:OptIn(DelicateCoroutinesApi::class)

package io.rippledown

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.OpenRDRUI
import kotlinx.coroutines.DelicateCoroutinesApi
import java.awt.event.WindowEvent
import java.lang.Thread.sleep
import javax.swing.JFrame
import javax.swing.SwingUtilities


class TestClientLauncher {
    private val handler = object : Handler {
        override var api = Api()
        override var isClosing: () -> Boolean = { false }
        override var setInfoMessage: (String) -> Unit = {}
    }
    private lateinit var composeWindow: ComposeWindow

    fun launchClient(): ComposeWindow {
        val api = Api()
        Thread {
            application {
                Window(
                    onCloseRequest = {
                        api.shutdown()
                    },
                    icon = painterResource("water-wave-icon.png"),
                    title = TITLE,
                    state = rememberWindowState(size = DpSize(1_400.dp, 800.dp))
                ) {
                    composeWindow = this.window
                    OpenRDRUI(handler)
                }
            }
            println("Compose window launch thread exiting")
        }.start()
        while (!::composeWindow.isInitialized) {
            sleep(100)
        }
        composeWindow.isAlwaysOnTop = true
        return composeWindow
    }

    fun stopClient() {
        handler.isClosing = { true }
        val frame = composeWindow as JFrame
        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
        SwingUtilities.invokeAndWait {
            frame.dispose()
        }
    }
}