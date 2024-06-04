package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeWindow
import io.rippledown.integration.utils.waitForWindowToShow

class RippleDownUIOperator(private val window: ComposeWindow) {
    init {
        window.waitForWindowToShow()
    }

    fun context() = window.accessibleContext

    fun applicationBarOperator(): ApplicationBarOperator {
        return ApplicationBarOperator {
            context()
        }
    }

    fun caseListPO() = CaseListPO {
        context()
    }
    fun caseCountPO() = CaseCountPO {
        context()
    }
    fun kbControlsPO(): KbControlsPO = KbControlsPO {
        context()
    }

    fun caseViewPO() = CaseViewPO {
        context()
    }

    fun cornerstonePO() = CornerstonePO {
        context()
    }

    fun interpretationViewPO() = InterpretationViewPO {
        context()
    }
    fun conclusionsViewPO() = ConclusionsViewPO {
        context()
    }

    fun ruleMakerPO() = RuleMakerPO {
        context()
    }

    fun shutdown() {
        window.accessibleContext.accessibleAction.doAccessibleAction(0)
    }
}