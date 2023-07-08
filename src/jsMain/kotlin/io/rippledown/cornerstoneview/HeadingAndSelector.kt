package io.rippledown.cornerstoneview

import Handler
import io.rippledown.constants.caseview.CASEVIEW_CORNERSTONE_CASE_NAME_ID
import mui.material.Stack
import mui.material.StackDirection.Companion.row
import mui.material.Typography
import mui.material.styles.TypographyVariant.Companion.subtitle2
import mui.system.responsive
import mui.system.sx
import px8
import react.FC
import web.cssom.AlignItems.Companion.center
import web.cssom.media.maxWidth
import web.cssom.px

external interface HeadingAndSelectorHandler : Handler {
    var name: String
    var numberOfCCs: Int
    var selectCornerstone: (index: Int) -> Unit
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
                handler.selectCornerstone(index)
            }
        }
    }
}

