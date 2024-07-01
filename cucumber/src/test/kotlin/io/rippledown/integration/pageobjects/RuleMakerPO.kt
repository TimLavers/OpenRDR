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
import io.rippledown.integration.utils.waitForContextToBeNotNull
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {

    private fun waitForAvailableConditionContextForIndex(index: Int) {
        waitUntilAsserted {
            availableConditionContextForIndex(index) shouldNotBe null
        }
    }

    private fun waitForSelectedConditionsContextForIndex(index: Int) {
        waitUntilAsserted {
            selectedConditionsContextForIndex(index) shouldNotBe null
        }
    }

    private fun waitForAvailableConditionsContext() {
        waitUntilAsserted {
            availableConditionsContext().size shouldBeGreaterThanOrEqualTo 1
        }
    }

    private fun waitForSelectedConditionsContext() {
        waitUntilAsserted {
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
        execute<AccessibleContext?> { contextProvider().find("$AVAILABLE_CONDITION_PREFIX$index") }

    private fun selectedConditionsContextForIndex(index: Int) =
        execute<AccessibleContext?> { contextProvider().find("$SELECTED_CONDITION_PREFIX$index") }

    private fun availableConditionsContext() =
        execute<Set<AccessibleContext>> { contextProvider().findAllByDescriptionPrefix(AVAILABLE_CONDITION_PREFIX) }

    private fun selectedConditionsContext() =
        execute<Set<AccessibleContext>> { contextProvider().findAllByDescriptionPrefix(SELECTED_CONDITION_PREFIX) }

    fun clickDoneButton() {
        waitForContextToBeNotNull(contextProvider, FINISH_RULE_BUTTON)
        execute { contextProvider().find(FINISH_RULE_BUTTON)!!.accessibleAction!!.doAccessibleAction(0) }
    }

    fun clickCancelButton() {
        waitForContextToBeNotNull(contextProvider, CANCEL_RULE_BUTTON)
        execute { contextProvider().find(CANCEL_RULE_BUTTON)!!.accessibleAction!!.doAccessibleAction(0) }
    }

    fun requireAvailableConditions(expectedConditions: List<String>) {
        waitForAvailableConditionContextForIndex(expectedConditions.size - 1)
        val contexts =
            execute<Set<AccessibleContext>> { contextProvider().findAllByDescriptionPrefix(AVAILABLE_CONDITION_PREFIX) }
        val found = contexts.map { it.accessibleName }
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