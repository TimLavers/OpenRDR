import csstype.*
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.key

external interface CaseViewHandler : Props {
    var case: RDRCase
    var onInterpretationSubmitted: (Interpretation) -> Unit
}

/**
 * A tabular representation of an RDRCase.
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { props ->
    div {
        key = props.case.name
        css {
            float = Float.left
            width = Length("70%")
            padding = px12
        }
        id = "case_view_container"
        div {
            +props.case.name
            id = "case_view_case_name"
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
            case = props.case
            onInterpretationSubmitted = props.onInterpretationSubmitted
        }
    }
}
