package io.rippledown.integration.pageobjects

import javax.accessibility.AccessibleContext

class CornerstoneCaseCountPO(contextProvider: () -> AccessibleContext): AbstractCaseCountPO(contextProvider) {
    override fun currentCount(): Int = snapshot().cornerstoneCount
    override fun isShowing(): Boolean = snapshot().isShowing && snapshot().cornerstoneCount > 0
}