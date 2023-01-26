package io.rippledown.kb

import Api
import io.rippledown.model.KBInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.FC
import react.Props
import react.useEffectOnce
import react.useState

external interface KBHandler: Props {
    var api: Api
}

const val ID_KB_INFO_HEADING = "kb_info_heading"
val mainScope = MainScope()

val KBInfoPane = FC<KBHandler> { handler ->
    var kbInfo by useState(KBInfo(""))

    useEffectOnce {
        mainScope.launch {
            kbInfo = handler.api.kbInfo()
        }
    }
    Grid {
        key = kbInfo.name
        container = true
        direction = responsive(GridDirection.row)
        spacing = responsive(2)
        Grid {
            item = true
            key = kbInfo.name
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
                reloadKB =  {
                    mainScope.launch {
                        kbInfo = handler.api.kbInfo()
                    }
                }
            }
        }
    }
}