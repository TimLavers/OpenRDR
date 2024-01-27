package io.rippledown

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.OpenRDRUI
import java.awt.event.WindowEvent
import javax.swing.JFrame


class TestClientLauncher {
    private lateinit var window: ComposeWindow

    fun launchClient(): ComposeWindow {
        val runnable = java.lang.Runnable {
            run {
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
                        this@run.window = this.window
                        OpenRDRUI(object : Handler {
                            override var api = api
                        })

                    }
                }
            }
        }
        Thread(runnable, "App Runner").start()

        var attempt = 0
        while (!::window.isInitialized && attempt++ < 50) {
            Thread.sleep(100)
        }
        return window
    }

    fun stopClient() {
        val frame = window as JFrame
        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
    }
}