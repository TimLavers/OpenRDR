package io.rippledown.integration.pageobjects

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.constants.rule.CANCEL_RULE_BUTTON
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.constants.rule.SELECTED_CONDITION_PREFIX
import io.rippledown.integration.pause
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.waitForComposeDialogToShow
import io.rippledown.integration.utils.waitForContextToBeNotNull
import io.rippledown.integration.waitUntilAsserted
import io.rippledown.main.LEFT_INFO_MESSAGE_ID
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import java.util.function.BiPredicate
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

    fun requireAvailableConditionsContains(conditions: Set<String>) {
        await().atMost(ofSeconds(2)).untilAsserted {
            val allShowing = allSuggestedConditions()
            conditions.forEach {
                allShowing shouldContain it
            }
        }
    }

    fun requireAvailableConditionsDoesNotContain(absentConditions: Set<String>) {
        await().atMost(ofSeconds(2)).untilAsserted {
            val allShowing = allSuggestedConditions()
            absentConditions.forEach {
                allShowing shouldNotContain it
            }
        }
    }

    private fun selectedConditions(): List<String> {
        waitForSelectedConditionsContext()
        return execute<Set<AccessibleContext>> {
            contextProvider().findAllByDescriptionPrefix(SELECTED_CONDITION_PREFIX)
        }.map { it.accessibleName }
    }

    fun requireSelectedConditions(selectedConditions: List<String>) {
        selectedConditions() shouldBe selectedConditions
    }

    fun requireSelectedConditionsDoesNotContain(conditions: Set<String>) {
        conditions.forEach {
            requireSelectedConditionsDoesNotContain(it)
        }
    }

    private fun requireSelectedConditionsDoesNotContain(condition: String) {
        selectedConditions() shouldNotContain condition
    }

    fun requireSelectedConditionsContains(conditions: Set<String>) {
        conditions.forEach {
            requireSelectedConditionsContains(it)
        }
    }

    fun requireSelectedConditionsContains(condition: String) {
        selectedConditions() shouldContain condition
    }

    fun clickConditionWithText(condition: String) {
        clickConditionMatchingText(condition) { t, u -> t == u }
    }

    fun clickConditionStartingWithText(condition: String) {
        clickConditionMatchingText(condition) { t, u -> t.startsWith(u) }
    }

    private fun clickConditionMatchingText(condition: String, matcher: BiPredicate<String, String>) {
        waitForAvailableConditionsContext()
        execute {
            val ctxt = availableConditionsContext().firstOrNull { it ->
//                println("Checking '$condition'")
//                println("    with '${it.accessibleName}'")
//                val match = it.accessibleName == condition
                val match = matcher.test(it.accessibleName, condition)
//                it.accessibleName == condition
//                println("match: $match")
                match
            }
            ctxt?.accessibleAction?.doAccessibleAction(0)
        }
    }

    fun setEditableValue(value: String) {
        val dialog = waitForComposeDialogToShow()
        with(EditConditionOperator(dialog)) {
            pause(100)
            enterValue(value)
            pause(100)
            clickOkButton()
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

    private fun allSuggestedConditions(): List<String> {
        return execute<Set<AccessibleContext>> {
            contextProvider().findAllByDescriptionPrefix(AVAILABLE_CONDITION_PREFIX)
        }.map { it.accessibleName }
    }

    fun requireMessageForAddingComment(newComment: String) {
        waitUntilAsserted {
            execute<String?> {
                contextProvider().find(LEFT_INFO_MESSAGE_ID)?.accessibleName
            } shouldBe "$ADDING$newComment"
        }
    }

    fun requireMessageForRemovingComment(originalComment: String) {
        waitUntilAsserted {
            execute<String?> {
                contextProvider().find(LEFT_INFO_MESSAGE_ID)?.accessibleName
            } shouldBe "$REMOVING$originalComment"
        }
    }

    fun requireMessageForReplacingComment(replacedComment: String, replacementComment: String) {
        waitUntilAsserted {
            execute<String?> {
                contextProvider().find(LEFT_INFO_MESSAGE_ID)?.accessibleName
            } shouldBe "$REPLACING$replacedComment$BY$replacementComment"
        }
    }
}