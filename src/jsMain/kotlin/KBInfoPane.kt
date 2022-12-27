import io.rippledown.model.KBInfo
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML.h2
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
    h2 {
        +"Knowledge Base: ${kbInfo.name}"
        id = ID_KB_INFO_HEADING
    }
}