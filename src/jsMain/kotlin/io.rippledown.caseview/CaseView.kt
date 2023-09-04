package io.rippledown.caseview

import Handler
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.interpretation.CASE_VIEW_CONTAINER
import io.rippledown.model.caseview.ViewableCase
import mui.material.Typography
import mui.system.sx
import px8
import react.FC
import react.dom.html.ReactHTML.div
import react.memo


external interface CaseViewHandler : Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
}

/**
 * A view of a Case, including its name and a table of its attributes and results.
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { handler ->
    div {
        //Re-render when the case changes
        key = caseTableKey(handler.case.id!!)
        id = CASE_VIEW_CONTAINER

        Typography {
            +handler.case.name
            id = CASEVIEW_CASE_NAME_ID
            sx {
                marginBottom = px8
            }
        }

        CaseTable {
            case = handler.case
            api = handler.api
            scope = handler.scope
            onCaseEdited = handler.onCaseEdited
        }
    }
}

fun caseTableKey(id: Long) = id.toString()

val CaseViewMemo = memo(
    type = CaseView,
    propsAreEqual = { oldProps, newProps ->
        oldProps.case.id == newProps.case.id
    }
)
