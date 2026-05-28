package io.rippledown.integration.pageobjects

import javax.accessibility.AccessibleContext

class CaseCountPO(contextProvider: () -> AccessibleContext): AbstractCaseCountPO(contextProvider) {
    override fun currentCount(): Int = snapshot().processedCount
    override fun isShowing(): Boolean = snapshot().processedCount > 0
}