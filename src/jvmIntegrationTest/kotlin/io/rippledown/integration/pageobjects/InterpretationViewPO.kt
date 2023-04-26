package io.rippledown.integration.pageobjects

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

// ORD2
class InterpretationViewPO(private val driver: WebDriver) {

    fun enterVerifiedText(text: String): InterpretationViewPO {
        val textArea = interpretationArea()
        textArea.sendKeys(text)
        return this
    }

    fun setInterpretationTextAndSubmit(text: String): InterpretationViewPO {
        enterVerifiedText(text)
        val sendButton = driver.findElement(By.id("send_interpretation_button"))
        sendButton.click()
        return this
    }

    fun interpretationText(): String {
        return interpretationArea().getAttribute("value")
    }

    fun requireInterpretationText(expected: String): InterpretationViewPO {
        interpretationArea().getAttribute("value") shouldBe expected
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
        driver.findElement(By.id("$id$row")).text shouldBe text
        return this
    }

    fun deleteAllText(): InterpretationViewPO {
        val textArea = driver.findElement(By.id(INTERPRETATION_TEXT_AREA))
        textArea.selectAllText()
        textArea.delete()
        return this
    }

    fun WebElement.selectAllText() = sendKeys(Keys.chord(Keys.CONTROL, "a"))


    fun WebElement.delete() = sendKeys(Keys.DELETE)
}