import csstype.*
import emotion.react.css
import io.rippledown.model.Interpretation
import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
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
    div {
        span {
            +kbInfo.name
            css {
                fontFamily = FontFamily.sansSerif
                fontWeight = FontWeight.bold
                color = rdBlue
                textAlign = TextAlign.left
            }
            id = ID_KB_INFO_HEADING
        }
        input {
//            +"Import"
            type = InputType.file
            id = "import_from_zip"
//            css {
//                marginLeft = px12
//                padding = px4
//                fontSize = px12
//            }
//            onClick = {
//
//            }
        }
    }
}