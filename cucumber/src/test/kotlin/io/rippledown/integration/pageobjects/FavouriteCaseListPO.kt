package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.FAVOURITES_SECTION_ID
import javax.accessibility.AccessibleContext

class FavouriteCaseListPO(contextProvider: () -> AccessibleContext) : AbstractCaseSectionListPO(contextProvider) {
    override fun sectionDescription() = FAVOURITES_SECTION_ID
}
