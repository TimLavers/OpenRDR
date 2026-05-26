package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.EDIT_KB_DESCRIPTION_BUTTON_TEXT
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.waitForComposeDialogToShow
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class EditCurrentKbControlPO(private val contextProvider: () -> AccessibleContext) {

    fun showDescriptionOperator(): KbDescriptionOperator {
        expandDropdownMenu()
        clickDropdownItem(EDIT_KB_DESCRIPTION_BUTTON_TEXT)
        return KbDescriptionOperator(waitForComposeDialogToShow())
    }

    private fun expandDropdownMenu() {
        // Find-and-click in a single EDT pass and retry: under Compose 1.11
        // the dropdown's accessibility node can be replaced by a freshly
        // composed equivalent between a separate find() and click(), so a
        // single-shot findAndClick races against the swap and NPEs.
        waitUntilAsserted {
            val clicked = execute<Boolean> {
                val node = contextProvider().find(KB_CONTROL_DROPDOWN_DESCRIPTION)
                    ?: return@execute false
                node.accessibleAction?.doAccessibleAction(0) ?: false
                true
            }
            clicked shouldBe true
        }
    }

    /**
     * Waits for the popup item to appear in the accessibility tree before
     * clicking it. The popup is realized asynchronously, so a single lookup
     * right after expanding can race ahead of it.
     */
    private fun clickDropdownItem(description: String) {
        val item = await().atMost(ofSeconds(10)).until(
            {
                execute<AccessibleContext?> {
                    contextProvider()
                        .find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
                        ?.find(description)
                }
            },
            { it != null }
        )!!
        execute { item.accessibleAction.doAccessibleAction(0) }
    }
}