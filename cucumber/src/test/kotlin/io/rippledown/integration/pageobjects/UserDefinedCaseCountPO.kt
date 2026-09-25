package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.userListSectionHeaderId
import io.rippledown.constants.caseview.userListSectionId
import javax.accessibility.AccessibleContext

class UserDefinedCaseCountPO(val name: String, contextProvider: () -> AccessibleContext) : AbstractCaseCountPO(contextProvider) {
    override fun sectionDescription() = userListSectionId(name)
    override fun contextDescription(): String = userListSectionHeaderId(name)
}