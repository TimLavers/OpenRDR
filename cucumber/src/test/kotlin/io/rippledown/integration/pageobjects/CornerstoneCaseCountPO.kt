package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CORNERSTONE_SECTION_HEADER_ID
import io.rippledown.integration.utils.find
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class CornerstoneCaseCountPO(contextProvider: () -> AccessibleContext): AbstractCaseCountPO(contextProvider) {
    override fun contextForCaseCount(): AccessibleContext? =
        execute<AccessibleContext?> { contextProvider().find(CORNERSTONE_SECTION_HEADER_ID) }
}