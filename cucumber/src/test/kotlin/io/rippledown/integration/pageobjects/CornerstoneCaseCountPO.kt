package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CORNERSTONE_SECTION_HEADER_ID
import io.rippledown.constants.caseview.CORNERSTONE_SECTION_ID
import javax.accessibility.AccessibleContext

class CornerstoneCaseCountPO(contextProvider: () -> AccessibleContext): AbstractCaseCountPO(contextProvider) {

    override fun sectionDescription() = CORNERSTONE_SECTION_ID
    override fun contextDescription(): String = CORNERSTONE_SECTION_HEADER_ID
}