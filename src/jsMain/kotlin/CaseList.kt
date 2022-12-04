import csstype.Cursor
import csstype.px
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
        id = CASELIST_ID

        Grid {
            item = true

            List {
                sx {
                    cursor = Cursor.pointer
                    width = 200.px
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

