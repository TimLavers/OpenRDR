package io.rippledown.caseview

import Handler
import emotion.react.css
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import mui.material.Box
import mui.material.Table
import mui.material.Typography
import px12
import react.FC
import web.cssom.*

/**
 * A read-only version of an RDRCase.
 */

external interface CornerstoneViewHandler : Handler {
    var case: ViewableCase
}

val CornerstoneView = FC<CaseViewHandler> { handler ->
    Box {
        key = handler.case.name
        id = "case_view_container"
        css {
            float = Float.left
            width = 70.pct
            padding = px12
        }

        Typography {
            +handler.case.name
            id = CASEVIEW_CASE_NAME_ID
        }
        Table {
            css {
                border = 1.px
                borderColor = rgb(128, 128, 128)
                borderStyle = LineStyle.solid
                marginBottom = px12
            }
            CaseTableHeader {
                dates = handler.case.dates
            }
            CaseTableBody {
                case = handler.case
                api = handler.api
                scope = handler.scope
                onCaseEdited = handler.onCaseEdited
            }
        }
        InterpretationTabs {
            scope = handler.scope
            api = handler.api
            interpretation = handler.case.interpretation
            refreshCase = handler.onCaseEdited
            onStartRule = { newInterpretation ->
                handler.onStartRule(newInterpretation)
            }
        }
    }
}