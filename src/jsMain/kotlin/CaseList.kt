import csstype.*
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

external interface CaseListHandler : Props {
    var caseIds: List<CaseId>
    var onCaseSelected: (String) -> Unit
    var currentCase: RDRCase?
}

val CaseList = FC<CaseListHandler> { props ->
    div {
        css {
            after {
                display = Display.table
                clear = Clear.both
            }
        }
        div {
            +"Cases "
            id = "case_list_container"
            css {
                className = "left_column"
                float = Float.left
                width = Length("10%")
                padding = Length("12px")
            }
            ul {
                css {
                    paddingInlineStart = Length("0px")
                }
                for (caseId in props.caseIds) {
                    li {
                        +caseId.name
                        id = caseId.name
                        css {
                            textDecorationLine = TextDecorationLine.underline
                            padding = Length("3px")
                            listStyle = ListStyle.none
                        }
                        onClick = {
                            props.onCaseSelected(caseId.name)
                        }
                    }
                }
            }
        }
        if (props.currentCase != null) {
            CaseView {
                case = props.currentCase!!
            }
        }
    }
}
