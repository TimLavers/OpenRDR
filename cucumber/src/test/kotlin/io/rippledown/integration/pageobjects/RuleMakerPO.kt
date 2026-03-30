package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted
import io.rippledown.main.LEFT_INFO_MESSAGE_ID
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {
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