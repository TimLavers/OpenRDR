package io.rippledown.caseview

import io.rippledown.main.Handler
import io.rippledown.main.px12
import io.rippledown.model.caseview.ViewableCase
import mui.material.Table
import mui.system.sx
import react.FC
import web.cssom.px

external interface CaseTableHandler: Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
}

val CaseTable = FC<CaseTableHandler> { handler ->
    Table {
        sx {
            marginBottom = px12
            minWidth = 500.px
        }
        CaseTableHeader {
            dates = handler.case.dates
        }
        CaseTableBody {
            case = handler.case
            api = handler.api
            scope = handler.scope
            onCaseEdited = handler.onCaseEdited //todo test this
        }
    }
}
