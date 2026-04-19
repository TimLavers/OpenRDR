package steps

import io.rippledown.TestClientLauncher
import io.rippledown.integration.pageobjects.RippleDownUIOperator
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

class LaunchedClient {
    private val testClientLauncher = TestClientLauncher()
    private val composeWindow = testClientLauncher.launchClient()
    private val rdUiOperator = RippleDownUIOperator(composeWindow)

    fun bringToFront() {
        SwingUtilities.invokeAndWait {
            // Windows can deny toFront() unless the window is de-iconified and
            // explicitly raised. alwaysOnTop toggling is a reliable workaround
            // in CI to force the window above other apps (e.g. the IDE).
            if (composeWindow.state == java.awt.Frame.ICONIFIED) {
                composeWindow.state = java.awt.Frame.NORMAL
            }
            val wasAlwaysOnTop = composeWindow.isAlwaysOnTop
            composeWindow.isAlwaysOnTop = true
            composeWindow.toFront()
            composeWindow.requestFocus()
            composeWindow.isAlwaysOnTop = wasAlwaysOnTop
        }
        // Small settle time for OS focus to take effect before Robot sends keys.
        Thread.sleep(100)
    }

    /**
     * Runs [block] with the compose window pinned as always-on-top so no other
     * window can steal OS-level focus mid-sequence (critical for sequences of
     * Robot mouse + key events that must all be delivered to the test UI).
     */
    fun <T> withWindowOnTop(block: () -> T): T {
        var wasAlwaysOnTop = false
        SwingUtilities.invokeAndWait {
            if (composeWindow.state == java.awt.Frame.ICONIFIED) {
                composeWindow.state = java.awt.Frame.NORMAL
            }
            wasAlwaysOnTop = composeWindow.isAlwaysOnTop
            composeWindow.isAlwaysOnTop = true
            composeWindow.toFront()
            composeWindow.requestFocus()
        }
        Thread.sleep(100)
        try {
            return block()
        } finally {
            SwingUtilities.invokeAndWait {
                composeWindow.isAlwaysOnTop = wasAlwaysOnTop
            }
        }
    }
    fun applicationBarPO() = rdUiOperator.applicationBarOperator()
    fun caseListPO() = rdUiOperator.caseListPO()
    fun cornerstoneCaseListPO() = rdUiOperator.cornerstoneCaseListPO()
    fun caseCountPO() = rdUiOperator.caseCountPO()
    fun kbControlsPO() = rdUiOperator.kbControlsPO()
    fun editCurrentKbControlPO() = rdUiOperator.editCurrentKbControlPO()
    fun caseViewPO() = rdUiOperator.caseViewPO()
    fun cornerstonePO() = rdUiOperator.cornerstonePO()
    fun interpretationViewPO() = rdUiOperator.interpretationViewPO()
    fun chatPO() = rdUiOperator.chatPO()
    fun ruleMakerPO() = rdUiOperator.ruleMakerPO()
    fun screenshot(file: File) {
        file.parentFile?.mkdirs()
        // Compose renders via Skia so Swing's printAll() produces an empty
        // frame. Bring the window to the front first and use Robot to grab
        // the actual rendered pixels.
        bringToFront()
        val image = Robot().createScreenCapture(composeWindow.bounds)
        ImageIO.write(image, "png", file)
    }

    fun stopClient() {
        testClientLauncher.stopClient()
    }
}