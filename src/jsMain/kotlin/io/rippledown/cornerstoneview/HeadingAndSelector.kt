package io.rippledown.cornerstoneview

import io.rippledown.constants.caseview.CASEVIEW_CORNERSTONE_CASE_NAME_ID
import io.rippledown.main.Handler
import io.rippledown.main.px8
import mui.material.Stack
import mui.material.StackDirection.Companion.row
import mui.material.Typography
import mui.material.styles.TypographyVariant.Companion.subtitle2
import mui.system.responsive
import mui.system.sx
import react.FC
import web.cssom.AlignItems.Companion.center
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
        }

        CornerstoneSelector {
            total = handler.numberOfCCs
            onSelect = { index ->
                handler.selectCornerstone(index)
            }
        }
    }
}

