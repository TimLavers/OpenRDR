package main

import io.rippledown.appbar.ApplicationBar
import io.rippledown.casecontrol.CasePoller
import kotlinx.coroutines.CoroutineScope
import react.FC
import react.Props

external interface Handler : Props {
    var scope: CoroutineScope
    var api: Api
}

val OpenRDRUI = FC<Handler> { handler ->

    ApplicationBar {

    }
//    h1 {
//        +MAIN_HEADING
//        css {
//            color = blue
//            textAlign = center
//            fontFamily = sansSerif
//        }
//        id = MAIN_HEADING_ID
//    }

    CasePoller {
        scope = handler.scope
        api = handler.api
    }
}