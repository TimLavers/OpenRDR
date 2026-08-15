package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.PROCESSED_SECTION_ID
import javax.accessibility.AccessibleContext

class ProcessedCaseListPO(contextProvider: () -> AccessibleContext) : AbstractCaseSectionListPO(contextProvider) {
    override fun sectionDescription() = PROCESSED_SECTION_ID
}
