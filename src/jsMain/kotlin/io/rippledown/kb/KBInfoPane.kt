package io.rippledown.kb

import io.rippledown.constants.kb.KB_INFO_CONTROLS_ID
import io.rippledown.constants.kb.KB_INFO_HEADING_ID
import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import main.Handler
import mui.material.Grid
import mui.material.GridDirection
import mui.material.Typography
import mui.material.TypographyAlign
import mui.material.styles.TypographyVariant
import mui.system.responsive
import mui.system.sx
import react.FC
import react.useEffectOnce
import react.useState
import web.cssom.Visibility
import web.cssom.Visibility.Companion.visible
import web.cssom.px

external interface KBInfoPaneHandler : Handler {
    var showKBInfo: Boolean
}

val KBInfoPane = FC<KBInfoPaneHandler> { handler ->
    var kbInfo: KBInfo? by useState(null)

    fun kbName() = if (kbInfo != null) kbInfo!!.name else ""

    useEffectOnce {
        handler.scope.launch {
            kbInfo = handler.api.kbInfo()
        }
    }
    Grid {
        id = KB_INFO_CONTROLS_ID
        key = kbName()
        container = true
        direction = responsive(GridDirection.row)
        spacing = responsive(2)
        sx {
            paddingBottom = 10.px
            visibility = if (handler.showKBInfo) visible else Visibility.hidden
        }
        Grid {
            item = true
            key = kbName()
            Typography {
                +kbName()
                id = KB_INFO_HEADING_ID
                variant = TypographyVariant.h6
                align = TypographyAlign.left
            }
        }
        Grid {
            item = true
            KBImportDialog {
                api = handler.api
                scope = handler.scope
                reloadKB = {
                    handler.scope.launch {
                        kbInfo = handler.api.kbInfo()
                    }
                }
            }
        }
        Grid {
            item = true
            KBExportDialog {
                api = handler.api
                scope = handler.scope
            }
        }
    }
}