import csstype.*
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

const val CASELIST_ID = "case_list_container"
const val CASE_ID_PREFIX = "case_list_item_"

external interface CaseListHandler : Props {
    var caseIds: List<CaseId>
    var onCaseSelected: (String) -> Unit
    var currentCase: RDRCase?
    var onCaseProcessed: suspend (Interpretation) -> Unit
    var scope: CoroutineScope
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
            id = CASELIST_ID
            css {
                className = "left_column"
                float = Float.left
                width = Length("10%")
                padding = px12
            }
            ul {
                css {
                    paddingInlineStart = px0
                    cursor = Cursor.default
                }
                for (caseId in props.caseIds) {
                    li {
                        +caseId.name
                        id = "$CASE_ID_PREFIX${caseId.name}"
                        css {
                            textDecorationLine = TextDecorationLine.underline
                            padding = px4
                            listStyle = ListStyle.none
                        }
                        onClick = {
                            props.onCaseSelected(caseId.id)
                        }
                    }
                }
            }
        }
        if (props.currentCase != null) {
            CaseView {
                case = props.currentCase!!
                onInterpretationSubmitted = { interpretation ->
                    props.scope.launch {
                        println("CaseList: onInterpretationSubmitted: $interpretation")
                        props.onCaseProcessed(interpretation)
                    }
                }
            }
        } else {
            NoCaseView()
        }
    }
}
