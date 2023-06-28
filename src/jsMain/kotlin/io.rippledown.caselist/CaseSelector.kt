package io.rippledown.caselist

import Handler
import io.rippledown.constants.caseview.CASE_SELECTOR_ID
import io.rippledown.model.CaseId
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
                id = "$CASE_ID_PREFIX${caseId.id}"
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