import csstype.*
import emotion.react.css
import io.rippledown.model.caseview.ViewableCase
import mui.material.Grid
import mui.material.GridDirection
import mui.system.responsive
import react.FC
import react.dom.html.ReactHTML.div
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
    div {
        key = props.case.name
        id = "case_view_container"
        css {
            float = Float.left
            width = 70.pct
            padding = px12
        }
        Grid {
            container = true
            direction = responsive(GridDirection.row)
            Grid {
                item = true
                +props.case.name
                id = CASEVIEW_CASE_NAME_ID
                xs = 12
                css {
                    paddingBottom = px4
                    paddingLeft = px8
                    color = rdBlue
                    fontStyle = FontStyle.italic
                    fontWeight = FontWeight.bold
                }
            }
            Grid {
                item = true
                xs = 12
                table {
                    css {
                        border = 1.px
                        borderColor = rgb(128, 128, 128)
                        borderStyle = LineStyle.solid
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
            }
            Grid {
                item = true
                xs = 12

                InterpretationView {
                    scope = props.scope
                    api = props.api
                    interpretation = props.case.interpretation
                }
            }
        }
    }
}
