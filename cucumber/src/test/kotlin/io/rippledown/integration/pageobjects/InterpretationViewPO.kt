package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.*
import io.rippledown.integration.utils.dumpToText
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitForDebounce
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.TEXT

// ORD2
class InterpretationViewPO(private val contextProvider: () -> AccessibleContext) {

    fun appendVerifiedText(text: String): InterpretationViewPO {
        val textArea = interpretationArea()
//        textArea.sendKeys(Keys.END)
        enterVerifiedText(text)
        return this
    }

    fun enterVerifiedText(text: String): InterpretationViewPO {
//        val textArea = interpretationArea()
//        textArea.sendKeys(text)
//        waitForDebounce()
        return this
    }

    fun interpretationText(): String {
        return contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT)?.accessibleName ?: ""
    }

    fun waitForInterpretationText(expected: String): InterpretationViewPO {
        await()
            .atMost(ofSeconds(5))
            .until {
                interpretationText() == expected
            }
        return this
    }

    fun waitForInterpretationTextToContain(expected: String) {
        await().atMost(ofSeconds(5)).until {
            println("interpretationText() = ${interpretationText()}")
            interpretationText().contains(expected)
        }
    }

    fun interpretationArea() {
        TODO()
    }

    fun selectChangesTab(): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).click()
        return this
    }

    fun selectConclusionsTab(): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CONCLUSIONS)).click()
        return this
    }

    fun selectOriginalTab(): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_ORIGINAL)).click()
        return this
    }

    fun requireOriginalTextInRow(row: Int, text: String) = requireTextInRow(DIFF_VIEWER_ORIGINAL, row, text)
    fun requireChangedTextInRow(row: Int, text: String) = requireTextInRow(DIFF_VIEWER_CHANGED, row, text)

    fun numberOfRows(): Int {
        TODO()
//        val table = driver.findElement(By.id(DIFF_VIEWER_TABLE))
//        return table.findElements(By.tagName("tr")).size
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
//        driver.findElement(By.id("$DIFF_VIEWER_BUILD_ICON$row")) shouldNotBe null
        return this
    }

    fun requireNoCheckBoxInRow(row: Int): InterpretationViewPO {
//        driver.findElements(By.id("$DIFF_VIEWER_BUILD_ICON$row")) shouldHaveSize 0
        return this
    }

    private fun requireTextInRow(
        id: String,
        row: Int,
        text: String
    ): InterpretationViewPO {
//        val findElement = driver.findElement(By.id("$id$row"))
//        findElement.text shouldBe text
        return this
    }

    fun deleteAllText(): InterpretationViewPO {
//        val textArea = driver.findElement(By.id(INTERPRETATION_TEXT_AREA))
//        textArea.selectAllText()
//        textArea.delete()
//        waitForDebounce()
        return this
    }

//    fun WebElement.selectAllText() = sendKeys(Keys.chord(Keys.CONTROL, "a"))

    fun requireChangesLabel(expected: String): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).text shouldBe expected
        return this
    }

    fun requireBadgeCount(expected: Int): InterpretationViewPO {
        waitForDebounce()
//        driver.findElement(By.id(INTERPRETATION_CHANGES_BADGE)).text shouldContain expected.toString()
        return this
    }

    fun requireNoBadge(): InterpretationViewPO {
//        driver.findElement(By.className(BADGE_INVISIBLE_CLASS)) shouldNotBe null
        return this
    }

//    fun WebElement.delete() = sendKeys(Keys.DELETE, Keys.BACK_SPACE)

    fun buildRule(row: Int): InterpretationViewPO {
//        driver.findElement(By.id("$DIFF_VIEWER_BUILD_ICON$row")).click()
        return this
    }
}