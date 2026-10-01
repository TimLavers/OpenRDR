package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.userListSectionId
import javax.accessibility.AccessibleContext

class UserDefinedCaseListPO(val name: String, contextProvider: () -> AccessibleContext) : AbstractCaseSectionListPO(contextProvider) {
    override fun sectionDescription() = userListSectionId(name)
}
