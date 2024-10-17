package io.rippledown.integration.utils

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.OpenRDRUI

class OpenRdUIProxy() {
    lateinit var appWindow: ComposeWindow

    init {
        //        SwingUtilities.invokeLater {
        val runnable = java.lang.Runnable {
            run {
                application {
                    Window(
                        onCloseRequest = ::exitApplication,
                        icon = painterResource("water-wave-icon.png"),
                        title = TITLE
                    ) {
                        appWindow = this.window
                        OpenRDRUI(object : Handler {
                            override var api = Api()
                            override var isClosing: () -> Boolean = { false }
                            override var setRightInfoMessage: (message: String) -> Unit = {}
                            override fun showingCornerstone(isShowingCornerstone: Boolean) {}
                        })
                    }
                }
            }
            appWindow.setVisible(true)
        }
        Thread(runnable, "App Runner").start()
        Thread.sleep(1000)
        val accessibleContext = appWindow.accessibleContext
        val name = accessibleContext.accessibleName
        val childCount = accessibleContext.accessibleChildrenCount
        println(childCount)
        val child0 = accessibleContext.getAccessibleChild(0)
        accessibleContext.dumpToText(0)
    }
}