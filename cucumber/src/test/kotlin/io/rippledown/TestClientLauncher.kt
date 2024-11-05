package io.rippledown

import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.awaitApplication
import io.ktor.client.engine.cio.*
import io.ktor.client.utils.*
import io.ktor.util.*
import io.rippledown.constants.main.TITLE
import io.rippledown.main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.event.WindowEvent
import java.awt.event.WindowEvent.WINDOW_CLOSING
import java.lang.Thread.sleep
import javax.swing.SwingUtilities

//val clientCIO = CIO.create()
var launchCount = 0
class TestClientLauncher {
    private val composeWindow: ComposeWindow = TestWindowManager.composeWindow
    private lateinit var thread: Thread
    private var shutdown = false

    fun launchClient(): ComposeWindow {
        val handler = object : Handler {
            //                    override var api = api
            override var isClosing: () -> Boolean = { shutdown }
            override var setRightInfoMessage: (String) -> Unit = {}
            override fun showingCornerstone(isShowingCornerstone: Boolean) {
//                if (isShowingCornerstone) resizeWindow(EXPANDED_WINDOW_SIZE) else resizeWindow(
//                    DEFAULT_WINDOW_SIZE
//                )
            }
        }
        composeWindow.setContent {
            OpenRDRUI(handler)
        }
//        val api = Api(clientCIO)
//        thread = Thread {
//            application(exitProcessOnExit = false) {
//                var windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }
//                fun resizeWindow(newSize: DpSize) {
//                    windowSize = newSize
//                }
//
//                    val handler = object : Handler {
//                        //                    override var api = api
//                        override var isClosing: () -> Boolean = { shutdown }
//                        override var setRightInfoMessage: (String) -> Unit = {}
//                        override fun showingCornerstone(isShowingCornerstone: Boolean) {
//                            if (isShowingCornerstone) resizeWindow(EXPANDED_WINDOW_SIZE) else resizeWindow(
//                                DEFAULT_WINDOW_SIZE
//                            )
//                        }
//                    }
////                    Window(
////                        onCloseRequest = {
////                        api.shutdown()
////                            exitApplication()
////                        },
////                        icon = painterResource("water-wave-icon.png"),
////                        title = TITLE,
////                        state = WindowState(size = windowSize)//allow for resizing
////                    ) {
////                        composeWindow = this.window
//                        OpenRDRUI(handler)
////                    }
//                }
//            }
//        thread.name = "TestLauncherThread_$launchCount"
//        launchCount++
//        println("launchClient for launcher with thread ${thread.name}")
//        thread.start()
//        while (!::composeWindow.isInitialized) {
//            sleep(100)
//        }
//        composeWindow.isAlwaysOnTop = true
        return composeWindow
    }

    fun stopClient() {
        println("stopClient for launcher with thread ${thread.name}")
        shutdown = true
        SwingUtilities.invokeAndWait {
            composeWindow.dispatchEvent(WindowEvent(composeWindow, WINDOW_CLOSING))
            composeWindow.dispose()
        }
        println("joining thread ${thread.name}")
        thread.join()
        println("joined thread ${thread.name}")
    }
}
object TestWindowManager {

    lateinit var composeWindow: ComposeWindow
    private var thread: Thread = Thread(null, kotlinx.coroutines.Runnable {
        println("----------- TestWindowManager thread run, about to call application.....")
        application(exitProcessOnExit = false) {
            println("------ setting window size etc")
            var windowSize by remember { mutableStateOf(DEFAULT_WINDOW_SIZE) }
            fun resizeWindow(newSize: DpSize) {
                windowSize = newSize
            }
            println("------ about to create  window")

                Window(
                    onCloseRequest = {
//                        api.shutdown()
                        exitApplication()
                    },
                    state = WindowState(size = windowSize),//allow for resizing
                    visible = true,
                    title = TITLE,
                    icon = painterResource("water-wave-icon.png"),
                    undecorated = false,
                    transparent = false,
                    resizable = true,
                    enabled = true,
                    focusable = true,
                    alwaysOnTop = true,

                ) {
                    Blah()
                }
//                {
//                    println("------ creating window, about to set window")
//                    composeWindow = this.window
//                    println("------ window set to ${this.window}")
//                    println("------ about to call blahj")
//                    Blah()
//                    println("Blah called ========")
//                    composeWindow = this.window
//                    println("------ window set to ${this.window}")
//                }
            }
//    }

    })
//    {

//        println("----------- TestWindowManager thread run, about to call appl.....")
    private var shutdown = false

    init {
//        val api = Api(clientCIO)
        thread.name = "TestLauncherThread_$launchCount"
        launchCount++
        println("launchClient for TestWindowManager with thread ${thread.name}, which has state ${thread.state}")
        thread.start()
        println("thread has state ${thread.state}")

        while (!::composeWindow.isInitialized) {
            println("--- waiting for window")
            sleep(1000)
        }
        println("--- got window")
        composeWindow.isAlwaysOnTop = true
    }

    fun stopClient() {
        println("stopClient for TestWindowManager with thread ${thread.name}")
        shutdown = true
        SwingUtilities.invokeAndWait {
            composeWindow.dispatchEvent(WindowEvent(composeWindow, WINDOW_CLOSING))
            composeWindow.dispose()
        }
        println("joining thread ${thread.name}")
        thread.join()
        println("joined thread ${thread.name}")
    }
}
@Composable
fun Blah() {
    println("---------------------------------- blah")
    Scaffold() {
        Text("Blsh")
    }
    println("---------------------------------- blah finished")

}
