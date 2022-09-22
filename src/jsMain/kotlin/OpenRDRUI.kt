import csstype.FontFamily
import csstype.TextAlign
import kotlinx.coroutines.CoroutineScope
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1

external interface OpenRDRUIHandler : Props {
    var scope: CoroutineScope
    var api: Api
}

val OpenRDRUI = FC<OpenRDRUIHandler> { handler ->
    div {
        css {
            fontFamily = FontFamily.sansSerif
        }
        h1 {
            +"Open RippleDown"
            css {
                color = rdBlue
                textAlign = TextAlign.center
            }
            id = "main_heading"
        }
        CaseQueue {
            scope = handler.scope
            api = handler.api
        }
    }
}