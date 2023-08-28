package io.rippledown.caseview

import Handler
import debug
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.interpretation.CASE_VIEW_CONTAINER
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import px12
import px8
import react.FC
import react.memo
import web.cssom.Float
import web.cssom.pct


external interface CaseViewHandler : Handler {
    var case: ViewableCase
    var currentInterpretation: Interpretation //Separate to case as we want to update it independently
    var onCaseEdited: () -> Unit
    var onStartRule: (selectedDiff: Diff) -> Unit
}

/**
 * A view of a Case and its Interpretation
 *
 *  ORD2
 */
val CaseView = FC<CaseViewHandler> { handler ->
    Stack {
        //Important! Force re-render when the case changes or its interpretation changes
        key = handler.case.id?.toString() + handler.currentInterpretation.toString()

        id = CASE_VIEW_CONTAINER
        sx {
            float = Float.left
            width = 80.pct
            padding = px12
        }

        Typography {
            +handler.case.name
            id = CASEVIEW_CASE_NAME_ID
            sx {
                marginBottom = px8
            }
        }

        Stack {
            CaseTable {
                case = handler.case
                api = handler.api
                scope = handler.scope
                onCaseEdited = handler.onCaseEdited
            }

            InterpretationTabs {
                scope = handler.scope
                api = handler.api
                interpretation = handler.currentInterpretation
                onStartRule = { selectedDiff ->
                    handler.onStartRule(selectedDiff)
                }
                isCornerstone = false
            }
        }
    }
}
val CaseViewMemo = memo(
    type = CaseView,
    propsAreEqual = { oldProps, newProps ->
        debug("CaseViewMemo: propsAreEqual(${oldProps.case.id}, ${newProps.case.id})")
        oldProps.case.id == newProps.case.id
    }
)
