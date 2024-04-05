package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CONCLUSIONS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS
import io.rippledown.integration.utils.find
import io.rippledown.interpretation.textContentDescription
import org.awaitility.Awaitility
import java.time.Duration
import javax.accessibility.AccessibleContext

class ConclusionsViewPO(private val contextProvider: () -> AccessibleContext) {

    fun selectConclusionsTab() {
        contextProvider().find(INTERPRETATION_TAB_CONCLUSIONS)!!.accessibleAction.doAccessibleAction(0)
    }

    fun clickClose() {
//        driver.findElement(By.id("conclusions_dialog_close")).click()
    }

    fun clickComment(comment: String) {
//        driver.findElement(By.ByXPath("//*[contains(@id, '$comment')]")).click()
    }

    private fun waitForConclusionsToShow() {
        Awaitility.await().pollDelay(Duration.ofSeconds(1)).untilAsserted {
            contextProvider().find(INTERPRETATION_PANEL_CONCLUSIONS) shouldNotBe null
        }
    }

    fun requireCommentAtIndex(index: Int, expected: String) {
        waitForConclusionsToShow()
        val contentDescription = textContentDescription(1, 0, index, expected)
        contextProvider().find(contentDescription) shouldNotBe null
    }

    fun requireConditionAtIndex(parentIndex: Int, index: Int, expected: String) {
        waitForConclusionsToShow()
        val contentDescription = textContentDescription(2, parentIndex, index, expected)
        contextProvider().find(contentDescription) shouldNotBe null
    }
}