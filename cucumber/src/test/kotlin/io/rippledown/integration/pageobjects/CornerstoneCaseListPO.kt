package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CORNERSTONE_SECTION_ID
import javax.accessibility.AccessibleContext

class CornerstoneCaseListPO(contextProvider: () -> AccessibleContext) : AbstractCaseSectionListPO(contextProvider) {
    override fun sectionDescription() = CORNERSTONE_SECTION_ID
}
