package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.PROCESSED_SECTION_HEADER_ID
import io.rippledown.constants.caseview.PROCESSED_SECTION_ID
import javax.accessibility.AccessibleContext

class CaseCountPO(contextProvider: () -> AccessibleContext) : AbstractCaseCountPO(contextProvider) {
    override fun contextDescription(): String = PROCESSED_SECTION_HEADER_ID

    override fun sectionDescription(): String = PROCESSED_SECTION_ID
}