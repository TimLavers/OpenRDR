package io.rippledown.casecontrol

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import io.rippledown.caseview.CaseView
import io.rippledown.caseview.CaseViewHandler
import io.rippledown.main.Handler
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff

interface CaseInspectionHandler : Handler, InterpretationTabsHandler {
    var case: ViewableCase
    var updateCase: (Long) -> Unit
    var ruleSessionInProgress: (Boolean) -> Unit
}

@Composable
fun CaseInspection(handler: CaseInspectionHandler) {
    Column {
        CaseView(handler = object : CaseViewHandler, Handler by handler {
            override var case: ViewableCase = handler.case
            override var caseEdited = {}
        })
        InterpretationTabs(handler.case.viewableInterpretation, object : InterpretationTabsHandler, Handler by handler {
            //                override var interpretation: ViewableInterpretation = handler.case.viewableInterpretation
                override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                override var isCornerstone: Boolean = false
            override var onInterpretationEdited: (text: String) -> Unit = { }
            })
    }
}
