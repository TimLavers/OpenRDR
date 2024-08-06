package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.INTERPRETATION_PANEL_CONCLUSIONS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS
import io.rippledown.constants.interpretation.NO_CONCLUSIONS
import io.rippledown.integration.utils.find
import io.rippledown.interpretation.textContentDescription
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

class ConclusionsViewPO(private val contextProvider: () -> AccessibleContext) {

    fun selectConclusionsTab() {
        execute { contextProvider().find(INTERPRETATION_TAB_CONCLUSIONS)!!.accessibleAction.doAccessibleAction(0) }
    }

    private fun waitForConclusionsToShow() {
        await().pollDelay(ofSeconds(1)).untilAsserted {
            execute<AccessibleContext?> { contextProvider().find(INTERPRETATION_PANEL_CONCLUSIONS) } shouldNotBe null
        }
    }

    fun requireCommentAtIndex(index: Int, expected: String) {
        waitForConclusionsToShow()
        val contentDescription = textContentDescription(1, 0, index, expected)
        execute<AccessibleContext?> { contextProvider().find(contentDescription) } shouldNotBe null
    }

    fun requireConditionAtIndex(parentIndex: Int, index: Int, expected: String) {
        waitForConclusionsToShow()
        val contentDescription = textContentDescription(2, parentIndex, index, expected)
        execute<AccessibleContext?> { contextProvider().find(contentDescription) } shouldNotBe null
    }

    fun requireNoComments() {
        execute<AccessibleContext?> { contextProvider().find(NO_CONCLUSIONS) } shouldNotBe null
    }
}