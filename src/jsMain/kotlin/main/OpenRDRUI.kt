package main

import emotion.react.css
import io.rippledown.casecontrol.CasePoller
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID
import kotlinx.coroutines.CoroutineScope
import react.FC
import react.Props
import react.dom.html.ReactHTML.h1
import web.cssom.FontFamily.Companion.sansSerif
import web.cssom.TextAlign.Companion.center

external interface Handler : Props {
    var scope: CoroutineScope
    var api: Api
}

val OpenRDRUI = FC<Handler> { handler ->

    h1 {
        +MAIN_HEADING
        css {
            color = blue
            textAlign = center
            fontFamily = sansSerif
        }
        id = MAIN_HEADING_ID
    }

    CasePoller {
        scope = handler.scope
        api = handler.api
    }
}