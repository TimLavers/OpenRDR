package io.rippledown.integration.utils

import io.rippledown.integration.waitForDebounce
import java.awt.Robot
import java.awt.event.KeyEvent

class Cyborg {

    private val robot = Robot()

    init {
        robot.isAutoWaitForIdle = true
        robot.autoDelay = 5
    }

    fun enter() = pressAndRelease(KeyEvent.VK_ENTER)

    fun tab() = pressAndRelease(KeyEvent.VK_TAB)

    fun type(text: String) = text.forEach {
        typeChar(it)
    }

    fun typeSlowly(text: String) = text.forEach {
        typeChar(it)
        waitForDebounce()
    }

    private fun typeChar(key: Char) {
        val keyCode = KeyEvent.getExtendedKeyCodeForChar(key.code)
        if (keyCode == KeyEvent.VK_UNDEFINED || keyCode == KeyEvent.VK_COLON) {
            typeSpecialChar(key)
        } else {
            try {
                pressAndRelease(keyCode)
            } catch (e: Exception) {
                println("Could not type '$key'. Key code is: $keyCode.")
            }
        }
    }

    private fun typeSpecialChar(key: Char) {
        when (key) {
            '!' -> pressAndReleaseShiftedKey(KeyEvent.VK_1)
            '@' -> pressAndReleaseShiftedKey(KeyEvent.VK_2)
            '#' -> pressAndReleaseShiftedKey(KeyEvent.VK_3)
            '$' -> pressAndReleaseShiftedKey(KeyEvent.VK_4)
            '%' -> pressAndReleaseShiftedKey(KeyEvent.VK_5)
            '^' -> pressAndReleaseShiftedKey(KeyEvent.VK_6)
            '&' -> pressAndReleaseShiftedKey(KeyEvent.VK_7)
            '*' -> pressAndReleaseShiftedKey(KeyEvent.VK_8)
            '(' -> pressAndReleaseShiftedKey(KeyEvent.VK_9)
            ')' -> pressAndReleaseShiftedKey(KeyEvent.VK_0)
            '_' -> pressAndReleaseShiftedKey(KeyEvent.VK_MINUS)
            '+' -> pressAndReleaseShiftedKey(KeyEvent.VK_EQUALS)
            '{' -> pressAndReleaseShiftedKey(KeyEvent.VK_OPEN_BRACKET)
            '}' -> pressAndReleaseShiftedKey(KeyEvent.VK_CLOSE_BRACKET)
            '|' -> pressAndReleaseShiftedKey(KeyEvent.VK_BACK_SLASH)
            ':' -> pressAndReleaseShiftedKey(KeyEvent.VK_SEMICOLON)
            '<' -> pressAndReleaseShiftedKey(KeyEvent.VK_COMMA)
            '>' -> pressAndReleaseShiftedKey(KeyEvent.VK_PERIOD)
            '?' -> pressAndReleaseShiftedKey(KeyEvent.VK_SLASH)
        }
    }

    private fun pressAndRelease(keyCode: Int) {
        robot.keyPress(keyCode)
        robot.delay(50)
        robot.keyRelease(keyCode)
        robot.delay(50)
    }

    private fun pressAndReleaseShiftedKey(keyCode: Int) {
        robot.keyPress(KeyEvent.VK_SHIFT)
        robot.delay(50)
        pressAndRelease(keyCode)
        robot.delay(50)
        robot.keyRelease(KeyEvent.VK_SHIFT)
        robot.delay(50)
    }
}