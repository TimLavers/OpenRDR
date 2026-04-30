package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.EDIT_KB_DESCRIPTION_BUTTON_TEXT
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.utils.waitForComposeDialogToShow
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
        execute { contextProvider().findAndClick(KB_CONTROL_DROPDOWN_DESCRIPTION) }
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