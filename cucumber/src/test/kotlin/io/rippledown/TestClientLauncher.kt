package io.rippledown

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.ktor.client.engine.cio.*
import io.rippledown.constants.main.TITLE
import io.rippledown.main.*
import java.awt.event.WindowEvent
import java.awt.event.WindowEvent.WINDOW_CLOSING
import java.lang.Thread.sleep
import javax.swing.SwingUtilities

val clientCIO = CIO.create()

class TestClientLauncher {

    private lateinit var composeWindow: ComposeWindow
    private lateinit var thread: Thread

    fun launchClient(): ComposeWindow {
        val api = Api(clientCIO)
        thread = Thread {
            application(exitProcessOnExit = false) {
                var windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }
                fun resizeWindow(newSize: DpSize) {
                    windowSize = newSize
                }

                val handler = object : Handler {
                    override var api = api
                    override var isClosing: () -> Boolean = { false }
                    override var setInfoMessage: (String) -> Unit = {}
                    override fun showingCornerstone(isShowingCornerstone: Boolean) {
                        if (isShowingCornerstone) resizeWindow(EXPANDED_WINDOW_SIZE) else resizeWindow(
                            DEFAULT_WINDOW_SIZE
                        )
                    }
                }
                Window(
                    onCloseRequest = {
                        api.shutdown()
                        exitApplication()
                    },
                    icon = painterResource("water-wave-icon.png"),
                    title = TITLE,
                    state = WindowState(size = windowSize)//allow for resizing
                ) {
                    composeWindow = this.window
                    OpenRDRUI(handler)
                }
            }
        }
        thread.start()
        while (!::composeWindow.isInitialized) {
            sleep(100)
        }
        composeWindow.isAlwaysOnTop = true
        return composeWindow
    }

    fun stopClient() {
//        handler.isClosing = { true }
        SwingUtilities.invokeAndWait {
            composeWindow.dispatchEvent(WindowEvent(composeWindow, WINDOW_CLOSING))
            composeWindow.dispose()
        }
        thread.join()
    }
}