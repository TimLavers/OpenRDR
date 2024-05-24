package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAssertedOnEventThread
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.TEXT

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {

    private fun waitForAvailableConditionContext(index: Int) {
        waitUntilAssertedOnEventThread {
            availableConditionContext(index) shouldNotBe null
        }
    }

    fun clickAvailableCondition(index: Int) {
        waitForAvailableConditionContext(index)
        execute { availableConditionContext(index)?.accessibleAction?.doAccessibleAction(0) }
    }

    private fun availableConditionContext(index: Int) = contextProvider().find("$AVAILABLE_CONDITION_PREFIX$index")

    fun clickDoneButton() =
        execute { contextProvider().find(FINISH_RULE_BUTTON, TEXT)!!.accessibleAction!!.doAccessibleAction(0) }

}