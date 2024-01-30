@file:OptIn(DelicateCoroutinesApi::class)

package io.rippledown

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.OpenRDRUI
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities


class TestClientLauncher {
    private lateinit var composeWindow: ComposeWindow

    fun launchClient(): ComposeWindow {
            GlobalScope.launch {
                val api = Api()
                application {
                    Window(
                        onCloseRequest = {
                            println("UI in test is closing")
                            api.shutdown()
                        },
                        icon = painterResource("water-wave-icon.png"),
                        title = TITLE
                    ) {
                        composeWindow = this.window
                        OpenRDRUI(object : Handler {
                            override var api = api
                        })
                    }
                }
            }

        var attempt = 0
        while (!::composeWindow.isInitialized && attempt++ < 50) {
            Thread.sleep(100)
        }
        return composeWindow
    }

    fun stopClient() {
        val frame = composeWindow as JFrame
        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
        SwingUtilities.invokeAndWait{
            frame.dispose()
        }
    }
}