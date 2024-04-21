package io.rippledown.casecontrol

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseView
import io.rippledown.caseview.CaseViewHandler
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff

interface CaseInspectionHandler : CaseViewHandler, InterpretationTabsHandler {
    var updateCase: (Long) -> Unit
    var ruleSessionInProgress: (Boolean) -> Unit
}

@Composable
fun CaseInspection(case: ViewableCase, handler: CaseInspectionHandler) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = androidx.compose.ui.Modifier
            .padding(10.dp)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        CaseView(case, handler = object : CaseViewHandler {
            override var caseEdited = {} //TODO
            override fun swapAttributes(moved: Attribute, target: Attribute) {
                handler.swapAttributes(moved, target)
            }
        })
        InterpretationTabs(case.viewableInterpretation, object : InterpretationTabsHandler {
            override var onStartRule: (selectedDiff: Diff) -> Unit = { }
            override var isCornerstone: Boolean = false
            override var onInterpretationEdited: (text: String) -> Unit = {
                handler.onInterpretationEdited(it)
            }
        })
    }
}
