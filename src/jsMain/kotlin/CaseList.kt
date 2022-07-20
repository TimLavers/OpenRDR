import api.getWaitingCasesInfo
import csstype.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul

private val scope = MainScope()

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
