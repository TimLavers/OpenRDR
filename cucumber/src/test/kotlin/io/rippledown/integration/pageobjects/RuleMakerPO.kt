package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.constants.rule.AVAILABLE_CONDITION_PREFIX
import io.rippledown.constants.rule.CANCEL_RULE_BUTTON
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.waitForContextToBeNotNull
import io.rippledown.integration.waitUntilAsserted
import io.rippledown.main.LEFT_INFO_MESSAGE_ID
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {

    private fun waitForAvailableConditionContextForIndex(index: Int) {
        waitUntilAsserted {
            availableConditionContextForIndex(index) shouldNotBe null
        }
    }

    fun clickAvailableCondition(index: Int) {
        waitForAvailableConditionContextForIndex(index)
        execute { availableConditionContextForIndex(index)?.accessibleAction?.doAccessibleAction(0) }
    }

    private fun availableConditionContextForIndex(index: Int) =
        execute<AccessibleContext?> { contextProvider().find("$AVAILABLE_CONDITION_PREFIX$index") }

    fun clickDoneButton() {
        waitForContextToBeNotNull(contextProvider, FINISH_RULE_BUTTON)
        execute { contextProvider().find(FINISH_RULE_BUTTON)!!.accessibleAction!!.doAccessibleAction(0) }
    }

    fun clickCancelButton() {
        waitForContextToBeNotNull(contextProvider, CANCEL_RULE_BUTTON)
        execute { contextProvider().find(CANCEL_RULE_BUTTON)!!.accessibleAction!!.doAccessibleAction(0) }
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