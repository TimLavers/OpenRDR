package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.FAVOURITES_SECTION_HEADER_ID
import io.rippledown.constants.caseview.FAVOURITES_SECTION_ID
import javax.accessibility.AccessibleContext

class FavouriteCaseCountPO(contextProvider: () -> AccessibleContext) : AbstractCaseCountPO(contextProvider) {
    override fun sectionDescription() = FAVOURITES_SECTION_ID
    override fun contextDescription(): String = FAVOURITES_SECTION_HEADER_ID
}