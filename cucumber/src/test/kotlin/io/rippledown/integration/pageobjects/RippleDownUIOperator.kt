package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeWindow
import io.rippledown.constants.caseview.CASE_HEADING
import io.rippledown.integration.utils.find
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

    fun caseListPO() = CaseListPO {
        context()
    }

    fun shutdown() {
        window.accessibleContext.accessibleAction.doAccessibleAction(0)
    }

    fun caseViewPO() = CaseViewPO {
        context()
    }

    fun interpretationViewPO() = InterpretationViewPO {
        context()
    }
}