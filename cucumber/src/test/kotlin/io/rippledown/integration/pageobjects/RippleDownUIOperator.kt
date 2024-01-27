package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeWindow
import io.rippledown.constants.main.APPLICATION_BAR_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.waitForWindowToShow
import javax.accessibility.AccessibleRole

class RippleDownUIOperator(private val window: ComposeWindow) {
    init {
        window.waitForWindowToShow()
    }

    fun applicationBarOperator() = ApplicationBarOperator(window.accessibleContext.find(APPLICATION_BAR_DESCRIPTION, AccessibleRole.GROUP_BOX)!!)

    fun shutdown() {
        window.accessibleContext.accessibleAction.doAccessibleAction(0)
    }
}