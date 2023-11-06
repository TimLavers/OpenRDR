package io.rippledown.appbar

import io.rippledown.constants.main.MAIN_HEADING_ID
import main.Handler
import mui.material.AppBar
import mui.material.Toolbar
import mui.material.Typography
import mui.material.TypographyAlign
import mui.material.styles.TypographyVariant
import react.FC

external interface AppBarHandler : Handler {
}

var ApplicationBar = FC<AppBarHandler> { handler ->
    AppBar {
        id = "app-bar"
        Toolbar {
            Typography {
                id = MAIN_HEADING_ID
                +"OpenRDR"
                variant = TypographyVariant.h6
                align = TypographyAlign.left
            }
        }
    }

}
