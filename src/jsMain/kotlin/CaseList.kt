import csstype.*
import emotion.react.css
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

const val CASELIST_ID = "case_list_container"
const val CASE_ID_PREFIX = "case_list_item_"
const val CASELIST_HEADING = "Cases"

external interface CaseListHandler : Handler {
    var caseIds: List<CaseId>
    var currentCase: RDRCase?
    var onCaseSelected: (String) -> Unit
    var onInterpretationSubmitted: () -> Unit
}

val CaseList = FC<CaseListHandler> { handler ->
    div {
        css {
            after {
                display = Display.table
                clear = Clear.both
            }
        }
        div {
            +CASELIST_HEADING
            id = CASELIST_ID
            css {
//                className = "left_column"
                float = Float.left
                width = 10.pct
                padding = px12
            }
            ul {
                css {
                    paddingInlineStart = px0
                    cursor = Cursor.default
                }
                for (caseId in handler.caseIds) {
                    li {
                        +caseId.name
                        id = "$CASE_ID_PREFIX${caseId.name}"
                        css {
                            textDecorationLine = TextDecorationLine.underline
                            padding = px4
//                            listStyle = ListStyle.none
                        }
                        onClick = {
                            handler.onCaseSelected(caseId.id)
                        }
                    }
                }
            }
        }
        if (handler.currentCase != null) {
            CaseView {
                scope = handler.scope
                api = handler.api
                case = handler.currentCase!!
                onInterpretationSubmitted = {
                    handler.onInterpretationSubmitted()
                }
            }
        } else {
            NoCaseView()
        }
    }
}
