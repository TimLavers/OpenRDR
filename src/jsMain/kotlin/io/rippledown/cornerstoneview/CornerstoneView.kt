package io.rippledown.cornerstoneview

import Handler
import debug
import io.rippledown.caseview.CaseTable
import io.rippledown.constants.caseview.CASEVIEW_CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.interpretation.CORNERSTONE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.EMPTY_CORNERSTONE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.NO_CORNERSTONES_TO_REVIEW
import io.rippledown.interpretation.InterpretationTabs
import io.rippledown.model.rule.CornerstoneStatus
import mui.material.Stack
import mui.material.StackDirection.Companion.row
import mui.material.Typography
import mui.material.styles.TypographyVariant.Companion.subtitle2
import mui.system.responsive
import mui.system.sx
import px12
import px8
import react.FC
import web.cssom.AlignItems.Companion.center
import web.cssom.media.maxWidth
import web.cssom.media.width
import web.cssom.pct
import web.cssom.px

/**
 * The view of the cornerstone case. The view itself is identical to CaseView, but there is no rule-building facility.
 */
external interface CornerstoneViewHandler : Handler {
    var cornerstoneStatus: CornerstoneStatus
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
        }

        CaseTable {
            case = cornerstone
            api = handler.api
            scope = handler.scope
        }

        InterpretationTabs {
            scope = handler.scope
            api = handler.api
            interpretation = cornerstone.interpretation
            isCornerstone = true
        }
    }
}

external interface HeadingAndSelectorHandler : Handler {
    var name: String
    var numberOfCCs : Int
}

val HeadingAndSelector = FC<HeadingAndSelectorHandler> { handler ->
    Stack {
        direction = responsive(row)
        spacing = responsive(20.px)
        sx {
            alignItems = center
            marginBottom = px8
        }

        Typography {
            variant = subtitle2
            +"Cornerstone"
        }

        Typography {
            +handler.name
            id = CASEVIEW_CORNERSTONE_CASE_NAME_ID
            noWrap = true
            maxWidth(10.px)
        }

        CornerstoneSelector {
            total = handler.numberOfCCs
            onSelect = { index ->
                debug("Selected index $index")
            }
        }
    }
}

