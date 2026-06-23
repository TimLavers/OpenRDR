package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CORNERSTONE_SECTION_HEADER_ID
import javax.accessibility.AccessibleContext

class CornerstoneCaseCountPO(contextProvider: () -> AccessibleContext): AbstractCaseCountPO(contextProvider) {

    override fun countTag() = "Cornerstones"
    override fun contextDescription(): String = CORNERSTONE_SECTION_HEADER_ID
}