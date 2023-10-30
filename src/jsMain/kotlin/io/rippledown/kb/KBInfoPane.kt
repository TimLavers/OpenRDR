package io.rippledown.kb

import Handler
import io.rippledown.constants.kb.KB_INFO_CONTROLS_ID
import io.rippledown.constants.kb.KB_INFO_HEADING_ID
import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import mui.material.Grid
import mui.material.GridDirection
import mui.material.Typography
import mui.material.TypographyAlign
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.FC
import react.useEffectOnce
import react.useState

external interface KBInfoPaneHandler : Handler

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