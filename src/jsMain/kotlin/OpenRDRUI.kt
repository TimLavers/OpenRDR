import emotion.react.css
import io.rippledown.kb.KBInfoPane
import kotlinx.coroutines.CoroutineScope
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import web.cssom.FontFamily
import web.cssom.TextAlign

external interface Handler : Props {
    var scope: CoroutineScope
    var api: Api
}

val OpenRDRUI = FC<Handler> { handler ->
    div {
        css {
            fontFamily = FontFamily.sansSerif
        }
        h1 {
            +"Open RippleDown"
            css {
                color = blue
                textAlign = TextAlign.center
            }
            id = "main_heading"
        }
        KBInfoPane{
            api = handler.api
        }
        CasePoller {
            scope = handler.scope
            api = handler.api
        }
    }
}