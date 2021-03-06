import api.getCase
import api.interpretationSubmitted
import csstype.*
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
    var onCaseProcessed: (String) -> Unit
}

private val scope = MainScope()

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
                padding = px12
            }
            ul {
                css {
                    paddingInlineStart = px0
                }
                for (caseId in props.caseIds) {
                    li {
                        +caseId.name
                        id = "case_list_item_${caseId.name}"
                        css {
                            textDecorationLine = TextDecorationLine.underline
                            padding = px4
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
                onInterpretationSubmitted = {
                    scope.launch {
                        interpretationSubmitted(it)
                        props.onCaseProcessed("")
                    }
                }
            }
        } else {
            NoCaseView()
        }
    }
}
