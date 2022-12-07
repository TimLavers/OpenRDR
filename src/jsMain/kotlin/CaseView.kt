import csstype.*
import emotion.react.css
import io.rippledown.model.caseview.ViewableCase
import mui.material.Box
import mui.material.Typography
import react.FC
import react.dom.html.ReactHTML.table

const val CASEVIEW_CASE_NAME_ID = "case_view_case_name"

external interface CaseViewHandler : Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
}

/**
 * A tabular representation of an RDRCase.
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { props ->
    Box {
        key = props.case.name
        id = "case_view_container"
        css {
            float = Float.left
            width = 70.pct
            padding = px12
        }

        Typography {
            +props.case.name
            id = CASEVIEW_CASE_NAME_ID
        }
        table {
            css {
                border = 1.px
                borderColor = rgb(128, 128, 128)
                borderStyle = LineStyle.solid
                marginBottom = px12
            }
            CaseTableHeader {
                dates = props.case.dates
            }
            CaseTableBody {
                case = props.case
                api = props.api
                scope = props.scope
                onCaseEdited = props.onCaseEdited
            }
        }
        InterpretationView {
            scope = props.scope
            api = props.api
            interpretation = props.case.interpretation
        }
    }
}
