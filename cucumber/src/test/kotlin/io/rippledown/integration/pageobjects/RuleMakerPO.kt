package io.rippledown.integration.pageobjects

import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.constants.rule.CANCEL_RULE_BUTTON
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.constants.rule.SELECTED_CONDITION_PREFIX
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.waitUntilAssertedOnEventThread
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {

    private fun waitForAvailableConditionContextForIndex(index: Int) {
        waitUntilAssertedOnEventThread {
            availableConditionContextForIndex(index) shouldNotBe null
        }
    }
    private fun waitForSelectedConditionsContextForIndex(index: Int) {
        waitUntilAssertedOnEventThread {
            selectedConditionsContextForIndex(index) shouldNotBe null
        }
    }

    private fun waitForAvailableConditionsContext() {
        waitUntilAssertedOnEventThread {
            availableConditionsContext().size shouldBeGreaterThanOrEqualTo 1
        }
    }

    private fun waitForSelectedConditionsContext() {
        waitUntilAssertedOnEventThread {
            selectedConditionsContext().size shouldBeGreaterThanOrEqualTo 1
        }
    }

    fun clickAvailableCondition(index: Int) {
        waitForAvailableConditionContextForIndex(index)
        execute { availableConditionContextForIndex(index)?.accessibleAction?.doAccessibleAction(0) }

    }

    fun clickSelectedCondition(index: Int) {
        waitForSelectedConditionsContextForIndex(index)
        execute { selectedConditionsContextForIndex(index)?.accessibleAction?.doAccessibleAction(0) }
    }

    private fun availableConditionContextForIndex(index: Int) =
        contextProvider().find("$AVAILABLE_CONDITION_PREFIX$index")

    private fun selectedConditionsContextForIndex(index: Int) =
        contextProvider().find("$SELECTED_CONDITION_PREFIX$index")

    private fun availableConditionsContext() =
        contextProvider().findAllByDescriptionPrefix(AVAILABLE_CONDITION_PREFIX)

    private fun selectedConditionsContext() =
        contextProvider().findAllByDescriptionPrefix(SELECTED_CONDITION_PREFIX)

    fun clickDoneButton() =
        execute { contextProvider().find(FINISH_RULE_BUTTON)!!.accessibleAction!!.doAccessibleAction(0) }

    fun clickCancelButton() =
        execute { contextProvider().find(CANCEL_RULE_BUTTON)!!.accessibleAction!!.doAccessibleAction(0) }

    fun requireAvailableConditions(expectedConditions: List<String>) {
        waitForAvailableConditionContextForIndex(expectedConditions.size - 1)
        val found = contextProvider().findAllByDescriptionPrefix(AVAILABLE_CONDITION_PREFIX).map { it.accessibleName }
        found shouldBe expectedConditions
    }

    fun clickConditionWithText(condition: String) {
        waitForAvailableConditionsContext()
        execute {
            availableConditionsContext().first {
                it.accessibleName == condition
            }.accessibleAction?.doAccessibleAction(0)
        }
    }

    fun removeConditionWithText(condition: String) {
        waitForSelectedConditionsContext()
        execute {
            selectedConditionsContext().first {
                it.accessibleName == condition
            }.accessibleAction?.doAccessibleAction(0)
        }
    }

}