package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CASE_SEARCH_SECTION_HEADER_ID
import io.rippledown.constants.caseview.CASE_SEARCH_SECTION_ID
import javax.accessibility.AccessibleContext

class CaseSearchCaseCountPO(contextProvider: () -> AccessibleContext) : AbstractCaseCountPO(contextProvider) {
    override fun sectionDescription() = CASE_SEARCH_SECTION_ID
    override fun contextDescription(): String = CASE_SEARCH_SECTION_HEADER_ID
}