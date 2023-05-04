import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.launch
import mui.material.Grid
import mui.material.List
import mui.material.ListItemButton
import mui.material.ListItemText
import mui.system.sx
import react.FC
import react.useEffect
import react.useState
import web.cssom.Cursor.Companion.pointer
import web.cssom.Overflow
import web.cssom.px

const val CASELIST_ID = "case_list_container"
const val CASE_ID_PREFIX = "case_list_item_"

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

    Grid {
        container = true

        Grid {
            item = true
            id = CASELIST_ID
            xs = 2
            List {
                sx {
                    cursor = pointer
                    width = 200.px
                    overflowY = Overflow.scroll
                    maxHeight = 500.px
                }
                dense = true

                for (caseId in handler.caseIds) {
                    ListItemButton {
                        ListItemText {
                            +caseId.name
                        }
                        selected = currentCase?.name == caseId.name
                        id = "$CASE_ID_PREFIX${caseId.name}"
                        onClick = {
                            handler.scope.launch {
                                currentCase = handler.api.getCase(caseId.id)
                            }
                        }
                        sx {
                            paddingTop = 0.px
                            paddingBottom = 0.px
                        }
                    }
                }
            }
        }
        Grid {
            item = true
            xs = 8
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
}

