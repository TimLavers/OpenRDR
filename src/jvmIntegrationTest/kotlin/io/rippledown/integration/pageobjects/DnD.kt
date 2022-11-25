package io.rippledown.integration.pageobjects

import io.rippledown.integration.pause
import org.openqa.selenium.By
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.awt.Robot
import java.awt.event.InputEvent

/**
 * Performs drag and drop of web elements
 * The current implementation uses a Robot since Selenium drag and drop doesn't work.
 */
class DnD(private val driver: WebDriver) {

    fun dragAttribute(draggedAttribute: String, targetAttribute: String) {
        val dragId = attributeCellId(draggedAttribute)
        val draggedElement = driver.findElement(By.id(dragId))
        val startPosition = getCentre(draggedElement)
        val targetId = attributeCellId(targetAttribute)
        val targetElement = driver.findElement(By.id(targetId))
        val endPosition = getCentre(targetElement)

        val robot = Robot()
        robot.autoDelay = 1
        robot.mouseMove(startPosition.x, startPosition.y)
        pause(200)
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        pause(200)
        val yDiff = endPosition.y - startPosition.y
        val step = if (yDiff < 0) -1 else 1
        val numberOfSteps = kotlin.math.abs(yDiff)
        var currentY = startPosition.y
        repeat(numberOfSteps) {
            currentY += step
            robot.mouseMove(startPosition.x, currentY) // Drag is vertical, so x is constant.
            pause(2)
        }
        pause(200)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    }

    private fun getCentre(webElement: WebElement): Point {
        val relativePosition = webElement.location
        val absolutePositionX = relativePosition.x
        val absolutePositionY = relativePosition.y + 120
        val rectangle = webElement.rect
        return Point(absolutePositionX + rectangle.width/2, absolutePositionY + rectangle.height/2)
    }

    private fun attributeCellId(attributeName: String?) = "attribute_name_cell_$attributeName"
}