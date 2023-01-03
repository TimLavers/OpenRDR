package io.rippledown.kb

import Handler
import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.FC
import react.useEffectOnce
import react.useState

external interface KBHandler : Handler

const val ID_KB_INFO_HEADING = "kb_info_heading"

val KBInfoPane = FC<KBHandler> { handler ->
    var kbInfo by useState(KBInfo(""))

    useEffectOnce {
        handler.scope.launch {
            kbInfo = handler.api.kbInfo()
        }
    }
    Grid {
        container = true
        direction = responsive(GridDirection.row)
        spacing = responsive(2)
        Grid {
            item = true
            Typography {
                +kbInfo.name
                id = ID_KB_INFO_HEADING
                variant = TypographyVariant.h6
                align = TypographyAlign.left

            }
        }
        Grid {
            item = true
            KBImportDialog {
                api = handler.api
                scope = handler.scope
            }
        }
    }
}