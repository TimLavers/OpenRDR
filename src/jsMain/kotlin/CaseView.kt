import emotion.react.css
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import mui.material.Box
import mui.material.Typography
import react.FC
import react.dom.html.ReactHTML.table
import web.cssom.LineStyle.Companion.solid
import web.cssom.pct
import web.cssom.px
import web.cssom.rgb

const val CASEVIEW_CASE_NAME_ID = "case_view_case_name"

external interface CaseViewHandler : Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
    var onStartRule: (interpretation: Interpretation) -> Unit
}

/**
 * A tabular representation of an RDRCase.
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { handler ->
    Box {
        key = handler.case.name
        id = "case_view_container"
        css {
            float = web.cssom.Float.left
            width = 70.pct
            padding = px12
        }

        Typography {
            +handler.case.name
            id = CASEVIEW_CASE_NAME_ID
        }
        table {
            css {
                border = 1.px
                borderColor = rgb(128, 128, 128)
                borderStyle = solid
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
