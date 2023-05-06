package io.rippledown.integration.pageobjects

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import io.rippledown.integration.pause
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

// ORD2
class InterpretationViewPO(private val driver: WebDriver) {

    fun enterVerifiedText(text: String): InterpretationViewPO {
        val textArea = interpretationArea()
        textArea.sendKeys(text)
        waitForDebounce()
        return this
    }

    fun interpretationText() = interpretationArea().getAttribute("value")

    fun requireInterpretationText(expected: String): InterpretationViewPO {
        interpretationText() shouldBe expected
        return this
    }

    fun interpretationArea() = driver.findElement(By.id(INTERPRETATION_TEXT_AREA))!!

    fun selectChangesTab(): InterpretationViewPO {
        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).click()
        return this
    }

    fun selectOriginalTab(): InterpretationViewPO {
        driver.findElement(By.id(INTERPRETATION_TAB_ORIGINAL)).click()
        return this
    }

    fun requireOriginalTextInRow(row: Int, text: String) = requireTextInRow(DIFF_VIEWER_ORIGINAL, row, text)
    fun requireChangedTextInRow(row: Int, text: String) = requireTextInRow(DIFF_VIEWER_CHANGED, row, text)

    fun numberOfRows(): Int {
        val table = driver.findElement(By.id(DIFF_VIEWER_TABLE))
        return table.findElements(By.tagName("tr")).size
    }

    fun requireNoRowsInDiffTable() = numberOfRows() shouldBe 0

    fun requireAddedText(text: String) {
        var found = false
        0.until(numberOfRows()).forEach { row ->
            try {
                requireOriginalTextInRow(row, "")
                requireAddedTextInRow(row, text)
                found = true
            } catch (e: AssertionError) {
                // Ignore
            }
        }
        if (!found) {
            throw AssertionError("Could not find added text '$text'")
        }
    }

    fun requireDeletedText(text: String) {
        var found = false
        0.until(numberOfRows()).forEach { row ->
            try {
                requireOriginalTextInRow(row, text)
                requireChangedTextInRow(row, "")
                found = true
            } catch (e: AssertionError) {
                // Ignore
            }
        }
        if (!found) {
            throw AssertionError("Could not find deleted text '$text'")
        }
    }

    fun requireReplacedText(replaced: String, replacement: String) {
        var found = false
        0.until(numberOfRows()).forEach { row ->
            try {
                requireOriginalTextInRow(row, replaced)
                requireChangedTextInRow(row, replacement)
                found = true
            } catch (e: AssertionError) {
                // Ignore
            }
        }
        if (!found) {
            throw AssertionError("Could not find replacement of '$replaced' with '$replacement'")
        }
    }

    fun requireAddedTextInRow(row: Int, text: String) {
        requireTextInRow(DIFF_VIEWER_ORIGINAL, row, "")
        requireTextInRow(DIFF_VIEWER_CHANGED, row, text)
        requireCheckBoxInRow(row)
    }

    fun requireCheckBoxInRow(row: Int): InterpretationViewPO {
        driver.findElement(By.id("$DIFF_VIEWER_CHECKBOX$row")) shouldNotBe null
        return this
    }

    fun requireNoCheckBoxInRow(row: Int): InterpretationViewPO {
        driver.findElements(By.id("$DIFF_VIEWER_CHECKBOX$row")) shouldHaveSize 0
        return this
    }

    private fun requireTextInRow(
        id: String,
        row: Int,
        text: String
    ): InterpretationViewPO {
        val findElement = driver.findElement(By.id("$id$row"))
        findElement.text shouldBe text
        return this
    }

    fun deleteAllText(): InterpretationViewPO {
        val textArea = driver.findElement(By.id(INTERPRETATION_TEXT_AREA))
        textArea.selectAllText()
        textArea.delete()
        waitForDebounce()
        return this
    }

    private fun waitForDebounce() {
        pause(2 * DEBOUNCE_WAIT_PERIOD_MILLIS)
    }

    fun WebElement.selectAllText() = sendKeys(Keys.chord(Keys.CONTROL, "a"))

    fun requireChangesLabel(expected: String): InterpretationViewPO {
        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).text shouldBe expected
        return this
    }

    fun requireBadgeCount(expected: Int): InterpretationViewPO {
        driver.findElement(By.className(BADGE_CLASS)).text.toInt() shouldBe expected
        return this
    }

    fun requireNoBadge(): InterpretationViewPO {
        driver.findElement(By.className(BADGE_INVISIBLE_CLASS)) shouldNotBe null
        return this
    }

    fun WebElement.delete() = sendKeys(Keys.DELETE)
}