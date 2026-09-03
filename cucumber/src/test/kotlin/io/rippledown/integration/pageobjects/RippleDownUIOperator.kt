package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeWindow
import io.rippledown.integration.utils.waitForWindowToShow

class RippleDownUIOperator(private val window: ComposeWindow) {
    init {
        window.waitForWindowToShow()
    }

    fun context() = window.accessibleContext

    fun applicationBarOperator() = ApplicationBarOperator {
        context()
    }

    fun caseListPO() = CaseListPO {
        context()
    }

    fun cornerstoneCaseListPO() = CornerstoneCaseListPO {
        context()
    }

    fun processedCaseListPO() = ProcessedCaseListPO {
        context()
    }

    fun favouriteCaseListPO() = FavouriteCaseListPO {
        context()
    }

    fun caseCountPO() = CaseCountPO {
        context()
    }

    fun cornerstoneCaseCountPO() = CornerstoneCaseCountPO {
        context()
    }

    fun favouriteCaseCountPO() = FavouriteCaseCountPO {
        context()
    }

    fun caseSearchCaseCountPO() = CaseSearchCaseCountPO {
        context()
    }

    fun kbControlsPO() = KbControlsPO {
        context()
    }

    fun editCurrentKbControlPO() = EditCurrentKbControlPO {
        context()
    }

    fun caseViewPO() = CaseViewPO {
        context()
    }

    fun cornerstonePO() = CornerstonePO {
        context()
    }

    fun interpretationViewPO() = InterpretationPO {
        context()
    }

    fun chatPO() = ChatPO {
        context()
    }

    fun reportPO() = ReportPO {
        context()
    }

    fun shutdown() {
        window.accessibleContext.accessibleAction.doAccessibleAction(0)
    }
}