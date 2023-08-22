import emotion.react.css
import io.rippledown.casecontrol.CasePoller
import io.rippledown.kb.KBInfoPane
import kotlinx.coroutines.CoroutineScope
import react.FC
import react.Fragment
import react.Props
import react.dom.html.ReactHTML.h1
import web.cssom.FontFamily.Companion.sansSerif
import web.cssom.TextAlign.Companion.center

external interface Handler : Props {
    var scope: CoroutineScope
    var api: Api
}

val OpenRDRUI = FC<Handler> { handler ->
    Fragment {
        h1 {
            +"Open RippleDown"
            css {
                color = blue
                textAlign = center
                fontFamily = sansSerif
            }
            id = "main_heading"
        }
        KBInfoPane {
            scope = handler.scope
            api = handler.api
        }
        CasePoller {
            scope = handler.scope
            api = handler.api
        }
    }
}