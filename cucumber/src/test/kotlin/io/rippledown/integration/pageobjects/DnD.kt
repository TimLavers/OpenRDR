package io.rippledown.integration.pageobjects

import io.rippledown.integration.pause
import java.awt.Point
import java.awt.Robot
import java.awt.event.InputEvent

/**
 * Performs drag and drop of web elements
 * The current implementation uses a Robot since Selenium drag and drop doesn't work.
 */
fun dragVertically(from: Point, to: Point) {
    val robot = Robot()
    robot.autoDelay = 1
    robot.mouseMove(from.x, from.y)
    pause(200)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    pause(200)
    val yDiff = to.y - from.y
    val step = if (yDiff < 0) -1 else 1
    val numberOfSteps = kotlin.math.abs(yDiff)
    var currentY = from.y
    repeat(numberOfSteps) {
        currentY += step
        robot.mouseMove(to.x, currentY) // Drag is vertical, so x is constant.
        pause(2)
    }
    pause(200)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
}
