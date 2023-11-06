package io.rippledown.casecontrol

import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.CASE_SELECTOR_ID
import io.rippledown.model.CaseId
import main.Handler
import mui.material.List
import mui.material.ListItemButton
import mui.material.ListItemText
import mui.system.sx
import react.FC
import web.cssom.Cursor.Companion.pointer
import web.cssom.Overflow.Companion.scroll
import web.cssom.px

external interface CaseSelectorHandler : Handler {
    var caseIds: List<CaseId>
    var selectedCaseName: String?
    var selectCase: (id: Long) -> Unit
}

val CaseSelector = FC<CaseSelectorHandler> { handler ->
    List {
        id = CASE_SELECTOR_ID
        sx {
            cursor = pointer
            overflowY = scroll
            width = 200.px
            maxHeight = 500.px
        }
        dense = true

        for (caseId in handler.caseIds) {
            ListItemButton {
                ListItemText {
                    +caseId.name
                }
                selected = handler.selectedCaseName == caseId.name
                id = "$CASE_NAME_PREFIX${caseId.name}"
                onClick = {
                    handler.selectCase(caseId.id!!)
                }
                sx {
                    paddingTop = 0.px
                    paddingBottom = 0.px
                }
            }
        }
    }
}