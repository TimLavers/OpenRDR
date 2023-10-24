package io.rippledown.cornerstoneview

import Handler
import io.rippledown.caseview.CaseTable
import io.rippledown.constants.interpretation.CORNERSTONE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.EMPTY_CORNERSTONE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.NO_CORNERSTONES_TO_REVIEW
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.model.rule.CornerstoneStatus
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import px12
import react.FC
import web.cssom.pct

/**
 * The view of the cornerstone case. The view itself is identical to CaseView, but there is no rule-building facility.
 */
external interface CornerstoneViewHandler : Handler {
    var cornerstoneStatus: CornerstoneStatus
    var selectCornerstone: (index: Int) -> Unit
}

val CornerstoneView = FC<CornerstoneViewHandler> { handler ->
    val cornerstone = handler.cornerstoneStatus.cornerstoneToReview

    if (cornerstone == null) {
        Typography {
            id = EMPTY_CORNERSTONE_VIEW_CONTAINER
            +NO_CORNERSTONES_TO_REVIEW
        }
        return@FC
    }

    Stack {
        id = CORNERSTONE_VIEW_CONTAINER
        sx {
            float = web.cssom.Float.left
            width = 70.pct
            padding = px12
        }

        HeadingAndSelector{
            name = cornerstone.name
            numberOfCCs = handler.cornerstoneStatus.numberOfCornerstones
            selectCornerstone = handler.selectCornerstone
        }

        CaseTable {
            case = cornerstone
            api = handler.api
            scope = handler.scope
        }

        InterpretationTabs {
            scope = handler.scope
            api = handler.api
            interpretation = cornerstone.viewableInterpretation
            isCornerstone = true
        }
    }
}


