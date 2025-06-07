package io.rippledown.casecontrol

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseView
import io.rippledown.caseview.CaseViewHandler
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

interface CaseInspectionHandler : CaseViewHandler, InterpretationViewHandler

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
        InterpretationView(
            interpretation = case.viewableInterpretation,
            showChangeIcon = !isRuleBuilding,
            handler = handler
        )
    }
}
