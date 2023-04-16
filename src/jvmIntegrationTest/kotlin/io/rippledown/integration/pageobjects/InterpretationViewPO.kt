package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.DIFF_VIEWER_CHANGED
import io.rippledown.constants.interpretation.DIFF_VIEWER_ORIGINAL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

// ORD2
class InterpretationViewPO(private val driver: WebDriver) {


    fun selectChangesTab(): InterpretationViewPO {
        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).click()
        return this
    }

    fun requireOriginalTextInRow(row: Int, text: String): InterpretationViewPO {
        driver.findElement(By.id("$DIFF_VIEWER_ORIGINAL$row")).text shouldBe text
        return this
    }

    fun requireNoChangedTextInRow(row: Int): InterpretationViewPO {
        driver.findElement(By.id("$DIFF_VIEWER_CHANGED$row")).text shouldBe ""
        return this
    }
}