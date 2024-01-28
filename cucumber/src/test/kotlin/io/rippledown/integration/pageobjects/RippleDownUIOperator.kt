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

    fun applicationBarOperator(): ApplicationBarOperator {
        println("--> RDUIO, aBO")
        return ApplicationBarOperator {
            context()
        }
    }

    fun context() = window.accessibleContext

    fun shutdown() {
        window.accessibleContext.accessibleAction.doAccessibleAction(0)
    }
}