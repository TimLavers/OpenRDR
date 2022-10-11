import csstype.*
import io.rippledown.model.RDRCase
import react.FC
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.table
import react.key

const val CASEVIEW_CASE_NAME_ID = "case_view_case_name"

external interface CaseViewHandler : Handler {
    var case: RDRCase
    var onInterpretationSubmitted: () -> Unit
}

/**
 * A tabular representation of an RDRCase.
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { props ->
    div {
        key = props.case.name
        id = "case_view_container"
        css {
            float = Float.left
            width = Length("70%")
            padding = px12
        }
        div {
            +props.case.name
            id = CASEVIEW_CASE_NAME_ID
            css {
                paddingBottom = px4
                paddingLeft = px8
                color = rdBlue
                fontStyle = FontStyle.italic
                fontWeight = FontWeight.bold
            }
        }
        table {
            css {
                border = Length("1px")
                borderColor = rgb(128, 128, 128)
                borderStyle = LineStyle.solid
            }
            CaseTableHeader {
                dates = props.case.dates
            }
            CaseTableBody {
                case = props.case
            }
        }
        InterpretationView {
            scope = props.scope
            api = props.api
            interpretation = props.case.interpretation
            onInterpretationSubmitted = {
                props.onInterpretationSubmitted()
            }
        }
    }
}
