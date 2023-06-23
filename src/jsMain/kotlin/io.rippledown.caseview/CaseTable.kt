package io.rippledown.caseview

import Handler
import io.rippledown.model.caseview.ViewableCase
import mui.material.Table
import mui.system.sx
import px12
import react.FC
import web.cssom.LineStyle
import web.cssom.LineStyle.Companion.solid
import web.cssom.px
import web.cssom.rgb

external interface CaseTableHandler: Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
}

val CaseTable = FC<CaseTableHandler> { handler ->
    Table {
        sx {
            border = 1.px
            borderColor = rgb(128, 128, 128)
            borderStyle = solid
            marginBottom = px12
        }
        CaseTableHeader {
            dates = handler.case.dates
        }
        CaseTableBody {
            case = handler.case
            api = handler.api
            scope = handler.scope
        }
    }
}
