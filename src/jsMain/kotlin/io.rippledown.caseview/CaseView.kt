package io.rippledown.caseview

import Handler
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.interpretation.CASE_VIEW_CONTAINER
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import px12
import px8
import react.FC
import web.cssom.pct


external interface CaseViewHandler : Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
    var onStartRule: (interpretation: Interpretation) -> Unit
}

/**
 * A view of a Case and its Interpretation
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { handler ->
    Stack {
        id = CASE_VIEW_CONTAINER
        sx {
            float = web.cssom.Float.left
            width = 70.pct
            padding = px12
        }

        Typography {
            +handler.case.name
            id = CASEVIEW_CASE_NAME_ID
            sx {
                marginBottom = px8
            }
        }

        CaseTable {
            case = handler.case
            api = handler.api
            scope = handler.scope
            onCaseEdited = handler.onCaseEdited
        }

        InterpretationTabs {
            scope = handler.scope
            api = handler.api
            interpretation = handler.case.interpretation
            refreshCase = handler.onCaseEdited
            onStartRule = { newInterpretation ->
                handler.onStartRule(newInterpretation)
            }
            isCornerstone = false
        }
    }
}
