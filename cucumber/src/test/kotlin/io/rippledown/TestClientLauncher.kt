package io.rippledown

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.ktor.client.engine.cio.*
import io.rippledown.constants.main.TITLE
import io.rippledown.main.*
import kotlinx.coroutines.Dispatchers.Unconfined
import java.awt.event.WindowEvent
import java.awt.event.WindowEvent.WINDOW_CLOSING
import java.lang.Thread.sleep
import javax.swing.SwingUtilities

val clientCIO = CIO.create()

/**
 * Compose Desktop's `Window` schedules `UpdateEffect` continuations on the EDT
 * that can run AFTER the `ComposeContainer` has been disposed during teardown
 * (e.g. `setComponentOrientation` reacting to a snapshot read). The result is a
 * harmless `IllegalArgumentException("ComposeContainer is disposed")` logged on
 * the EDT after a scenario finishes. There is no public API to drain these
 * pending effects, so we install a default uncaught-exception handler that
 * swallows this one specific exception and delegates everything else.
 *
 * Idempotent: only installs the filter once per JVM.
 */
@Volatile
private var composeDisposalFilterInstalled = false

private fun installComposeDisposalExceptionFilter() {
    if (composeDisposalFilterInstalled) return
    composeDisposalFilterInstalled = true
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        if (throwable is IllegalArgumentException &&
            throwable.message == "ComposeContainer is disposed"
        ) {
            // Known-harmless Compose teardown race; ignore.
            return@setDefaultUncaughtExceptionHandler
        }
        previous?.uncaughtException(thread, throwable) ?: throwable.printStackTrace()
    }
}

class TestClientLauncher {

    private lateinit var composeWindow: ComposeWindow
    private lateinit var thread: Thread

    fun launchClient(): ComposeWindow {
        val api = Api(clientCIO)
        installComposeDisposalExceptionFilter()
        thread = Thread {
            application(exitProcessOnExit = false) {
                var windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }
                fun resizeWindow(newSize: DpSize) {
                    windowSize = newSize
                }

                val handler = object : Handler {
                    override var api = api
                    override var isClosing: () -> Boolean = { false }
                    override fun setWindowSize(isShowingCornerstone: Boolean) {
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
                    title = TITLE,
                    state = WindowState(size = windowSize)//allow for resizing
                ) {
                    composeWindow = this.window
                    applyAppIcon(this.window)
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }
            }
        }
        thread.start()
        while (!::composeWindow.isInitialized) {
            sleep(100)
        }
        return composeWindow
    }

    fun stopClient() {
        // Dispatch WINDOW_CLOSING so onCloseRequest runs (api.shutdown + exitApplication),
        // letting Compose's application{} block tear the Window down on its own.
        // Calling composeWindow.dispose() here races with Compose's pending UpdateEffects
        // (e.g. setComponentOrientation), producing
        // "IllegalArgumentException: ComposeContainer is disposed" on the EDT.
        SwingUtilities.invokeAndWait {
            composeWindow.dispatchEvent(WindowEvent(composeWindow, WINDOW_CLOSING))
        }
        thread.join()
    }
}