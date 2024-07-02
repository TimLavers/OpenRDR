package io.rippledown.casecontrol

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseView
import io.rippledown.caseview.CaseViewHandler
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff

interface CaseInspectionHandler : CaseViewHandler, InterpretationTabsHandler

@Composable
fun CaseInspection(case: ViewableCase, isRuleBuilding: Boolean, handler: CaseInspectionHandler) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 5.dp)
            .width(500.dp)
    ) {
        if (isRuleBuilding) Spacer(modifier = Modifier.height(40.dp))
        CaseView(case, handler = object : CaseViewHandler {
            override fun swapAttributes(moved: Attribute, target: Attribute) {
                handler.swapAttributes(moved, target)
            }
        })
        InterpretationTabs(case.viewableInterpretation, object : InterpretationTabsHandler {
            override fun onStartRule(selectedDiff: Diff) = handler.onStartRule(selectedDiff)
            override var isCornerstone: Boolean = false
            override var onInterpretationEdited: (text: String) -> Unit = {
                handler.onInterpretationEdited(it)
            }
        })
    }
}
