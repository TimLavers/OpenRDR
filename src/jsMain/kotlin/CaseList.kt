import csstype.*
import emotion.react.css
import io.rippledown.model.CaseId
import kotlinx.coroutines.launch
import io.rippledown.model.caseview.ViewableCase
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.useEffect
import react.useState

const val CASELIST_ID = "case_list_container"
const val CASE_ID_PREFIX = "case_list_item_"
const val CASELIST_HEADING = "Cases"

external interface CaseListHandler : Handler {
    var caseIds: List<CaseId>
}

val CaseList = FC<CaseListHandler> { handler ->
    var currentCase: ViewableCase? by useState(null)

    useEffect {
        val names = handler.caseIds.map { it.name }
        val currentCaseNullOrNotAvailable = currentCase == null || !names.contains(currentCase?.name)
        if (currentCaseNullOrNotAvailable && names.isNotEmpty()) {
            val firstCaseId = handler.caseIds[0]
            handler.scope.launch {
                val rdrCase = handler.api.getCase(firstCaseId.id)
                currentCase = rdrCase
            }
        }
    }

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
                textAlign = TextAlign.left
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
                            listStyleType = None.none
                        }
                        onClick = {
                            handler.scope.launch {
                                currentCase = handler.api.getCase(caseId.id)
                            }
                        }
                    }
                }
            }
        }
        if (currentCase != null) {
            CaseView {
                scope = handler.scope
                api = handler.api
                case = currentCase!!
                onCaseEdited = {
                    scope.launch {
                        val id = currentCase!!.name
                        currentCase = api.getCase(id)
                        case = currentCase!!
                    }
                }
            }
        }
    }
}
