package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.PROCESSED_SECTION_HEADER_ID
import io.rippledown.integration.utils.find
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class CaseCountPO(contextProvider: () -> AccessibleContext): AbstractCaseCountPO(contextProvider) {
    override fun contextForCaseCount(): AccessibleContext? =
        execute<AccessibleContext?> { contextProvider().find(PROCESSED_SECTION_HEADER_ID) }
}