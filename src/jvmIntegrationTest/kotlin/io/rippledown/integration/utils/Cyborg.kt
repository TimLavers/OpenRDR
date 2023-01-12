package io.rippledown.integration.utils

import java.awt.Robot
import java.awt.event.KeyEvent

class Cyborg {
    private val robot = Robot()

    init {
        robot.isAutoWaitForIdle = true
        robot.autoDelay = 5
    }

    fun enterText(text: String) {
        text.toCharArray().forEach { typeChar(it) }
    }

    fun enter() {
        pressAndRelease(KeyEvent.VK_ENTER)
    }

    private fun typeChar(key: Char) {
        pressAndRelease(KeyEvent.getExtendedKeyCodeForChar(key.code))
    }

    private fun pressAndRelease(keyCode: Int) {
        robot.keyPress(keyCode)
        robot.delay(50)
        robot.keyRelease(keyCode)
        robot.delay(50)
    }
}