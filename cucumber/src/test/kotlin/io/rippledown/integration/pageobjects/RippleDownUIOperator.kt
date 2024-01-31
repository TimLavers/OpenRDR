package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeWindow
import io.rippledown.integration.utils.waitForWindowToShow

class RippleDownUIOperator(private val window: ComposeWindow) {
    init {
        window.waitForWindowToShow()
    }

    fun context() = window.accessibleContext

    fun applicationBarOperator(): ApplicationBarOperator {
        println("--> RDUIO, aBO")
        return ApplicationBarOperator {
            context()
        }
    }

    fun caseListPO() = CaseListPO(context())

    fun shutdown() {
        window.accessibleContext.accessibleAction.doAccessibleAction(0)
    }
}