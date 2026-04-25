package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseView
import io.rippledown.caseview.CaseViewHandler
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff

interface CaseInspectionHandler : CaseViewHandler, InterpretationViewHandler

@Composable
fun CaseInspection(
    case: ViewableCase,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList(),
    handler: CaseInspectionHandler,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 5.dp)
    ) {
        CaseView(
            case = case,
            handler = object : CaseViewHandler {
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            },
            modifier = Modifier.weight(1f, fill = false)
        )
        InterpretationView(
            interpretation = case.viewableInterpretation,
            diff = diff,
            ruleConditions = ruleConditions,
            handler = handler
        )
    }
}
