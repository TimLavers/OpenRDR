package io.rippledown.caselist

import Handler
import debug
import io.rippledown.model.CaseId
import mui.material.List
import mui.material.ListItemButton
import mui.material.ListItemText
import mui.system.sx
import react.FC
import web.cssom.Cursor
import web.cssom.Overflow
import web.cssom.px

external interface CaseSelectorHandler : Handler {
    var caseIds: List<CaseId>
    var selectedCaseName: String?
    var selectCase: (id: String) -> Unit
}

val CaseSelector = FC<CaseSelectorHandler> { handler ->
    List {
        sx {
            cursor = Cursor.pointer
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
                selected = handler.selectedCaseName == caseId.name
                debug("item ${caseId.name} selected: $selected")
                id = "$CASE_ID_PREFIX${caseId.name}"
                onClick = {
                    handler.selectCase(caseId.name)
                }
                sx {
                    paddingTop = 0.px
                    paddingBottom = 0.px
                }
            }
        }
    }
}